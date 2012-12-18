package ee.lutsu.alpha.mc.mytown.event;

import java.lang.reflect.Field;
import java.util.logging.Level;

import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.network.IChatListener;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.commands.CmdChat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Block;
import net.minecraft.src.BlockRail;
import net.minecraft.src.EntityMinecart;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.TileEntity;
import net.minecraft.src.WorldInfo;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.minecart.MinecartCollisionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class PlayerEvents implements IPlayerTracker
{
	@ForgeSubscribe
	public void interact(PlayerInteractEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);
		
		if (ev.action == Action.RIGHT_CLICK_AIR)
			return;
		
		Permissions perm = Permissions.Build;
		if (ev.action == Action.RIGHT_CLICK_BLOCK && ev.entityPlayer.getHeldItem() == null)
			perm = Permissions.Access;
		
		// TODO: cart ids to config
		if (ev.action == Action.RIGHT_CLICK_BLOCK && ev.entityPlayer.getHeldItem().getItem() == Item.minecartEmpty)
		{
			int en = ev.entityPlayer.worldObj.getBlockId(ev.x , ev.y, ev.z);
			if (Block.blocksList[en] instanceof BlockRail)
			{
				TownBlock targetBlock = MyTownDatasource.instance.getBlock(ev.entityPlayer.dimension, (int)ev.x >> 4, (int)ev.z >> 4);

				if ((targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction) || ((targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(ev.entityPlayer.dimension).allowCartInteraction))
					return;
			}
		}
		
		if (!r.canInteract(ev.x , ev.y, ev.z, perm))
		{
			if (perm == Permissions.Access)
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotAccessHere.toString());
			else
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotBuildHere.toString());
				
			ev.setCanceled(true);
			
			// TODO: Remove. Fixed in Forge 1.4.5
			try
			{
				Field f = Event.class.getDeclaredField("isCanceled");
				f.setAccessible(true);
				f.set(ev, true);
			}
			catch(Exception e)
			{
				Log.severe("Failed Forge 1.4.2 PlayerInteractEvent bug workaround", e);
			}
		}	
	}
	
	@ForgeSubscribe
	public void pickup(EntityItemPickupEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.item))
		{
			long time = System.currentTimeMillis();
			if (time > r.pickupWarningCooldown)
			{
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotPickup.toString());
				r.pickupWarningCooldown = time + Resident.pickupSpamCooldown;
			}
			ev.setCanceled(true);
		}
	}
	
	@ForgeSubscribe
	public void entityAttack(AttackEntityEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident attacker = source().getOrMakeResident(ev.entityPlayer);

		if (!attacker.canAttack(ev.target))
		{
			ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotAttack.toString());
			ev.setCanceled(true);
		}
	}
	
	@ForgeSubscribe
	public void entityInteract(EntityInteractEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.target))
		{
			ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotInteract.toString());
			ev.setCanceled(true);
		}
	}
	
	@ForgeSubscribe
	public void minecartCollision(MinecartCollisionEvent ev)
	{
		if (!(ev.collider instanceof EntityPlayer))
			return;
		
		Resident r = source().getOrMakeResident((EntityPlayer)ev.collider);
		
		TownBlock t = source().getBlock(r.onlinePlayer.dimension, ev.minecart.chunkCoordX, ev.minecart.chunkCoordZ);
		
		if (t == null || t.town() == null || t.town() == r.town() || t.settings.allowCartInteraction)
			return;
		
		long time = System.currentTimeMillis();
		if (t.town().minecraftNotificationTime < time)
		{
			t.town().minecraftNotificationTime = time + Town.dontSendCartNotification;
			t.town().sendNotification(Level.WARNING, Term.MinecartMessedWith.toString());
		}
	}

	private MyTownDatasource source(){ return MyTownDatasource.instance; }

	@Override
	public void onPlayerLogin(EntityPlayer player) 
	{
		// load the resident
		Resident r = source().getOrMakeResident(player);
		
		TownBlock t = source().getBlock(r.onlinePlayer.dimension, player.chunkCoordX, player.chunkCoordZ);

		r.location = t != null && t.town() != null ? t.town() : null;
		r.location2 = t != null && t.town() != null ? t.owner() : null;
		
		if (!r.canByPassBounce() && !r.canInteract(t, Permissions.Enter))
		{
			Log.warning(String.format("Player %s logged in at a enemy town %s (%s, %s, %s) with bouncing on. Sending to spawn.",
					r.name(), r.location.name(),
					player.posX, player.posY, player.posZ));
			r.sendToSpawn();
		}
		
		if (r.town() != null)
			r.town().notifyPlayerLoggedOn(r);
		
		r.loggedIn();
	}

	@Override
	public void onPlayerLogout(EntityPlayer player) 
	{
		Resident res = source().getOrMakeResident(player);

		if (res.town() != null)
			res.town().notifyPlayerLoggedOff(res);
		
		res.loggedOf();
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player) {
	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {
	}

	@ForgeSubscribe
	public void serverChat(ServerChatEvent ev)
	{
		if (ev.isCanceled() || ev.message == null || ev.message.trim().length() < 1)
			return;
		
		ev.setCanceled(true);
		Resident res = source().getOrMakeResident(ev.player);
		CmdChat.sendToChannel(res, ev.message, res.activeChannel);
	}
	
	@ForgeSubscribe
	public void livingUpdate(LivingUpdateEvent ev)
	{
		if (ev.isCanceled() || !(ev.entityLiving instanceof EntityPlayer))
			return;

		// so we don't re-link to player to be online
		// as this is called after the player logs off
		Resident res = source().getResident((EntityPlayer)ev.entityLiving);
		
		if (res != null && res.isOnline())
			res.update();
	}
}
