package ee.lutsu.alpha.mc.mytown.event;

import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
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
		
		if (ev.action == Action.RIGHT_CLICK_AIR) // buckets second call
		{
			if (ev.entityPlayer.getHeldItem() == null) // nothing in hand
				return;
			
	        MovingObjectPosition var3 = getMovingObjectPositionFromPlayer(ev.entityPlayer.worldObj, ev.entityPlayer, true);
	        if (var3 == null)
	        	return;
	        
        	if ((var3.entityHit != null && !r.canAttack(var3.entityHit)) || (var3.entityHit == null && !r.canInteract(var3.blockX, var3.blockY, var3.blockZ, Permissions.Build)))
        	{
    			ev.setCanceled(true);
    			return;
        	}
		}
		
		Permissions perm = Permissions.Build;
		if (ev.action == Action.RIGHT_CLICK_BLOCK && ItemIdRange.contains(MyTown.instance.carts, ev.entityPlayer.getHeldItem()))
		{
			int en = ev.entityPlayer.worldObj.getBlockId(ev.x , ev.y, ev.z);
			if (Block.blocksList[en] instanceof BlockRail)
			{
				TownBlock targetBlock = MyTownDatasource.instance.getBlock(ev.entityPlayer.dimension, ChunkCoord.getCoord(ev.x), ChunkCoord.getCoord(ev.z));

				if ((targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction) || ((targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(ev.entityPlayer.dimension).allowCartInteraction))
					return;
			}
		}
		else if (ev.action == Action.RIGHT_CLICK_BLOCK && (ev.entityPlayer.getHeldItem() == null || ItemIdRange.contains(MyTown.instance.safeItems, ev.entityPlayer.getHeldItem())))
			perm = Permissions.Access;

		if (!r.canInteract(ev.x , ev.y, ev.z, perm))
		{
			ev.setCanceled(true);
			if (perm == Permissions.Access)
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotAccessHere.toString());
			else
				ev.entityPlayer.sendChatToPlayer(Term.ErrPermCannotBuildHere.toString());
		}	
	}
	
    public static MovingObjectPosition getMovingObjectPositionFromPlayer(World par1World, EntityPlayer par2EntityPlayer, boolean par3)
    {
        float var4 = 1.0F;
        float var5 = par2EntityPlayer.prevRotationPitch + (par2EntityPlayer.rotationPitch - par2EntityPlayer.prevRotationPitch) * var4;
        float var6 = par2EntityPlayer.prevRotationYaw + (par2EntityPlayer.rotationYaw - par2EntityPlayer.prevRotationYaw) * var4;
        double var7 = par2EntityPlayer.prevPosX + (par2EntityPlayer.posX - par2EntityPlayer.prevPosX) * (double)var4;
        double var9 = par2EntityPlayer.prevPosY + (par2EntityPlayer.posY - par2EntityPlayer.prevPosY) * (double)var4 + 1.62D - (double)par2EntityPlayer.yOffset;
        double var11 = par2EntityPlayer.prevPosZ + (par2EntityPlayer.posZ - par2EntityPlayer.prevPosZ) * (double)var4;
        Vec3 var13 = par1World.getWorldVec3Pool().getVecFromPool(var7, var9, var11);
        float var14 = MathHelper.cos(-var6 * 0.017453292F - (float)Math.PI);
        float var15 = MathHelper.sin(-var6 * 0.017453292F - (float)Math.PI);
        float var16 = -MathHelper.cos(-var5 * 0.017453292F);
        float var17 = MathHelper.sin(-var5 * 0.017453292F);
        float var18 = var15 * var16;
        float var20 = var14 * var16;
        double var21 = 5.0D;
        if (par2EntityPlayer instanceof EntityPlayerMP)
        {
            var21 = ((EntityPlayerMP)par2EntityPlayer).theItemInWorldManager.getBlockReachDistance();
        }
        Vec3 var23 = var13.addVector((double)var18 * var21, (double)var17 * var21, (double)var20 * var21);
        return par1World.rayTraceBlocks_do_do(var13, var23, par3, !par3);
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
