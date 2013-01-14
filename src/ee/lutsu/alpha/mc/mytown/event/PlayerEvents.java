package ee.lutsu.alpha.mc.mytown.event;

import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemPotion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.minecart.MinecartCollisionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import cpw.mods.fml.common.IPlayerTracker;
import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Utils;
import ee.lutsu.alpha.mc.mytown.commands.CmdChat;
import ee.lutsu.alpha.mc.mytown.entities.ItemIdRange;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;

public class PlayerEvents implements IPlayerTracker
{
	@ForgeSubscribe
	public void interact(PlayerInteractEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);
		
		if (!ProtectionEvents.instance.itemUsed(r))
		{
			ev.setCanceled(true);
			r.onlinePlayer.stopUsingItem();
			return;
		}
		Permissions perm = Permissions.Build;
		
		if (ev.action == Action.RIGHT_CLICK_AIR) // entity or air click
		{
			if (ev.entityPlayer.getHeldItem() != null && ev.entityPlayer.getHeldItem().getItem() != null)
			{
				Item item = ev.entityPlayer.getHeldItem().getItem();
				MovingObjectPosition pos = Utils.getMovingObjectPositionFromPlayer(r.onlinePlayer.worldObj, r.onlinePlayer, false);
				if (pos == null)
				{
					if (item instanceof ItemBow || item instanceof ItemEgg || item instanceof ItemPotion || item instanceof ItemFishingRod || item instanceof ItemExpBottle || item instanceof ItemEnderEye)
					{
						perm = Permissions.Build;
					}
					else
						return;
					
					ev = new PlayerInteractEvent(ev.entityPlayer, ev.action, (int)ev.entityPlayer.posX, (int)ev.entityPlayer.posY, (int)ev.entityPlayer.posZ, ev.face);
				}
				else
				{
					if (pos.typeOfHit == EnumMovingObjectType.ENTITY)
						ev = new PlayerInteractEvent(ev.entityPlayer, ev.action, (int)pos.entityHit.posX, (int)pos.entityHit.posY, (int)pos.entityHit.posZ, ev.face);
					else
						ev = new PlayerInteractEvent(ev.entityPlayer, ev.action, pos.blockX, pos.blockY, pos.blockZ, ev.face);
				}
			}
			else
				return;
		}
		else if (ev.action == Action.RIGHT_CLICK_BLOCK && (ev.entityPlayer.getHeldItem() != null && ev.entityPlayer.getHeldItem().getItem() != null && ev.entityPlayer.getHeldItem().getItem() instanceof ItemMinecart))
		{
			int en = ev.entityPlayer.worldObj.getBlockId(ev.x , ev.y, ev.z);
			if (Block.blocksList[en] instanceof BlockRail)
			{
				TownBlock targetBlock = MyTownDatasource.instance.getBlock(ev.entityPlayer.dimension, ChunkCoord.getCoord(ev.x), ChunkCoord.getCoord(ev.z));
				if (targetBlock != null && targetBlock.settings.yCheckOn)
				{
					if (ev.y < targetBlock.settings.yCheckFrom || ev.y > targetBlock.settings.yCheckTo)
						targetBlock = targetBlock.getFirstFullSidingClockwise(targetBlock.town());
				}
				
				if ((targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction) || ((targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(ev.entityPlayer.dimension).allowCartInteraction))
					return;
			}
		}
		else if (ev.action == Action.RIGHT_CLICK_BLOCK)
		{
			if (!r.onlinePlayer.isSneaking())
			{
				TileEntity te = r.onlinePlayer.worldObj.getBlockTileEntity(ev.x , ev.y, ev.z);
				if (te != null && te instanceof IInventory && ((IInventory)te).isUseableByPlayer(r.onlinePlayer))
					perm = Permissions.Access;
			}
		}
		/*else if (ev.action == Action.RIGHT_CLICK_BLOCK && (ev.entityPlayer.getHeldItem() == null || ev.entityPlayer.getHeldItem().getItem() == null || !(ev.entityPlayer.getHeldItem().getItem() instanceof ItemBlock)))
			perm = Permissions.Access;*/

		if (!r.canInteract(ev.x , ev.y, ev.z, perm))
		{
			r.onlinePlayer.stopUsingItem();
			ev.setCanceled(true);
			if (perm == Permissions.Access)
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotAccessHere.toString());
			else
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotBuildHere.toString());
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
		
		if (!r.canInteract(t, (int)player.posY, Permissions.Enter))
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
