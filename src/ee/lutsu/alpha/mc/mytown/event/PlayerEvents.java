package ee.lutsu.alpha.mc.mytown.event;

import java.util.logging.Level;

import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.network.IChatListener;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.commands.CmdChat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WorldInfo;
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
		
		if (!r.canInteract(ev.x , ev.y, ev.z))
			ev.setCanceled(true);
	}
	
	@ForgeSubscribe
	public void pickup(EntityItemPickupEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.item))
			ev.setCanceled(true);
	}
	
	@ForgeSubscribe
	public void entityAttack(AttackEntityEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident attacker = source().getOrMakeResident(ev.entityPlayer);

		if (!attacker.canAttack(ev.target))
			ev.setCanceled(true);
	}
	
	@ForgeSubscribe
	public void entityInteract(EntityInteractEvent ev)
	{
		if (ev.isCanceled())
			return;
		
		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.target))
			ev.setCanceled(true);
	}
	
	@ForgeSubscribe
	public void minecartCollision(MinecartCollisionEvent ev)
	{
		if (!(ev.collider instanceof EntityPlayer))
			return;
		
		Resident r = source().getOrMakeResident((EntityPlayer)ev.collider);
		
		TownBlock t = source().getBlock(r.onlinePlayer.dimension, ev.minecart.chunkCoordX, ev.minecart.chunkCoordZ);
		
		if (t == null || t.town() == null || t.town() == r.town())
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
		
		if (r.location != null && r.location != r.town() && r.location.bounceNonMembers && !r.isOp())
		{
			Log.log(String.format("[MyTown] Player %s logged in at a enemy town %s (%s, %s, %s) with bouncing on. Sending to spawn.",
					r.name(), r.location.name(),
					player.posX, player.posY, player.posZ));
			r.sendToSpawn();
		}
	}

	@Override
	public void onPlayerLogout(EntityPlayer player) 
	{
		Resident res = source().getOrMakeResident(player);
		res.firstTick = true;
		res.onlinePlayer = null;
		
		if (res.town() == null)
			source().unloadResident(res);
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player) {
	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {
	}
	
	@ForgeSubscribe
	public void enterChunk(EnteringChunk ev)
	{
		if (!(ev.entity instanceof EntityPlayer))
			return;
		
		Resident res = source().getOrMakeResident((EntityPlayer)ev.entity);
		if (res.onlinePlayer == null)
			return;
		
		if (res.beingBounced)
			return;
		
		TownBlock block = source().getBlock(ev.entity.dimension, ev.newChunkX, ev.newChunkZ);
		
		if (block == null && res.location != null)
		{
			// entered wild
			res.onlinePlayer.sendChatToPlayer(Term.PlayerEnteredWild.toString());
			res.location = null;
		}
		else if (block != null && block.town() != null && res.location != block.town())
		{
			// entered town or another town
			if (block.town() != res.town())
			{
				if (block.town().bounceNonMembers && !res.isOp())
				{
					res.beingBounced = true;
					try
					{
						res.onlinePlayer.sendChatToPlayer(Term.TownYouCannotEnter.toString(block.town().name()));
						res.bounceAway(ev.oldChunkX, ev.oldChunkZ, ev.newChunkX, ev.newChunkZ);
						
						TownBlock block2 = source().getBlock(ev.entity.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ);
						if (block2 != null && block2.town() != null && block2.town() != res.town() && block2.town().bounceNonMembers)
						{
							// bounce failed, send to spawn
							Log.log(String.format("[MyTown] Player %s is inside a enemy town %s (%s, %s, %s) with bouncing on. Sending to spawn.",
									res.name(), block2.town().name(),
									res.onlinePlayer.posX, res.onlinePlayer.posY, res.onlinePlayer.posZ));
							
							res.sendToSpawn();
						}
					}
					finally
					{
						res.beingBounced = false;
					}
				}
				else
				{
					res.location = block.town();
					res.onlinePlayer.sendChatToPlayer(Term.PlayerEnteredTown.toString(block.town().name()));
				}
			}
			else
			{
				res.location = block.town();
				res.onlinePlayer.sendChatToPlayer(Term.PlayerEnteredOwnTown.toString(block.town().name()));
			}
		}
		
		if (res.mapMode)
			res.sendLocationMap(ev.entity.dimension, ev.newChunkX, ev.newChunkZ);
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

		Resident res = source().getOrMakeResident((EntityPlayer)ev.entityLiving);
		res.update();
	}
}
