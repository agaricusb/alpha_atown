package ee.lutsu.alpha.mc.mytown.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.event.prot.SteveCarts;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class ProtectionEvents implements ITickHandler
{
	Class clLaser = null;
	Field fTickInAir, fOwner, fExplosive;

	public boolean enabled = false, laserEnabled = false, steveCartsRailerEnabled = false;
	public static ProtectionEvents instance = new ProtectionEvents();
	public int explosionRadius = 6;
	Resident lastOwner = null;

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) 
	{
		if (!enabled)
			return;
		
		setFields();
		
		World world = (World)tickData[0];
		Entity e = null;
		String kill = null;
		ArrayList<Entity> toRemove = new ArrayList<Entity>();
		
		try
		{
			for (int i = 0; i < world.loadedEntityList.size(); i++)
			{
				e = (Entity)world.loadedEntityList.get(i);
				lastOwner = null;
				kill = null;
				
				if (laserEnabled && e.getClass() == clLaser)
					kill = handleLaserEntityUpdate(e);
				else if (steveCartsRailerEnabled && SteveCarts.instance.isSteveEntity(e))
					kill = SteveCarts.instance.update(e);
				
				if (kill != null)
				{
					if (lastOwner != null)
					{
						Log.severe(String.format("Player %s tried to bypass at dim %d, %d,%d,%d using %s - %s", lastOwner.name(), lastOwner.onlinePlayer.dimension, (int)lastOwner.onlinePlayer.posX, (int)lastOwner.onlinePlayer.posY, (int)lastOwner.onlinePlayer.posZ, e.toString(), kill));
						
						if (lastOwner.isOnline())
							lastOwner.onlinePlayer.sendChatToPlayer("§4You cannot use that here - " + kill);
					}
					toRemove.add(e);
				}
			}
			
			for (Entity en : toRemove)
				world.removeEntity(en);
		}
		catch(Exception er)
		{
			String ms = e == null ? "#unknown#" : e.toString();
			throw new RuntimeException("Error in entity " + ms + " pre-update check", er);
		}
	}
	
	private void setFields()
	{
		if (clLaser == null && laserEnabled)
		{
			try
			{
				clLaser = Class.forName("ic2.common.EntityMiningLaser");
				fOwner = clLaser.getDeclaredField("owner");
				fTickInAir = clLaser.getDeclaredField("ticksInAir");
				fExplosive = clLaser.getDeclaredField("explosive");
			}
			catch (Exception e)
			{
				throw new RuntimeException("ProtectionEvents.clLaser cannot bind to 'ic2.common.EntityMiningLaser'. Is IC2 loaded?", e);
			}
		}
		if (!SteveCarts.instance.loaded() && steveCartsRailerEnabled)
		{
			try
			{
				SteveCarts.instance.load();
			}
			catch (Exception e)
			{
				throw new RuntimeException("ProtectionEvents.clSteveCart cannot bind to 'vswe.stevescarts.entMCBase'. Is SteveCarts loaded?", e);
			}
		}
	}
	
	private String handleLaserEntityUpdate(Entity e) throws IllegalArgumentException, IllegalAccessException
	{
		fTickInAir.setAccessible(true);
		EntityPlayer owner = (EntityPlayer)fOwner.get(e); // actually living
		Integer ticksInAir = (Integer)fTickInAir.get(e);
		Boolean explosive = (Boolean)fExplosive.get(e);
		
		if (owner == null)
			return "no owner";

		Resident res = lastOwner = MyTownDatasource.instance.getOrMakeResident(owner);
		
        Vec3 var1 = Vec3.createVectorHelper(e.posX, e.posY, e.posZ);
        Vec3 var2 = Vec3.createVectorHelper(e.posX + e.motionX, e.posY + e.motionY, e.posZ + e.motionZ);
        MovingObjectPosition var3 = e.worldObj.rayTraceBlocks_do_do(var1, var2, false, true);
        var1 = Vec3.createVectorHelper(e.posX, e.posY, e.posZ);

        if (var3 != null)
        {
            var2 = Vec3.createVectorHelper(var3.hitVec.xCoord, var3.hitVec.yCoord, var3.hitVec.zCoord);
        }
        else
        {
            var2 = Vec3.createVectorHelper(e.posX + e.motionX, e.posY + e.motionY, e.posZ + e.motionZ);
        }

        Entity var4 = null;
        List var5 = e.worldObj.getEntitiesWithinAABBExcludingEntity(e, e.boundingBox.addCoord(e.motionX, e.motionY, e.motionZ).expand(1.0D, 1.0D, 1.0D));
        double var6 = 0.0D;
        int var8;

        for (var8 = 0; var8 < var5.size(); ++var8)
        {
            Entity var9 = (Entity)var5.get(var8);

            if (var9.canBeCollidedWith() && (var9 != owner || ticksInAir >= 5))
            {
                float var10 = 0.3F;
                AxisAlignedBB var11 = var9.boundingBox.expand((double)var10, (double)var10, (double)var10);
                MovingObjectPosition var12 = var11.calculateIntercept(var1, var2);

                if (var12 != null)
                {
                    double var13 = var1.distanceTo(var12.hitVec);

                    if (var13 < var6 || var6 == 0.0D)
                    {
                        var4 = var9;
                        var6 = var13;
                    }
                }
            }
        }

        if (var4 != null)
            var3 = new MovingObjectPosition(var4);

        if (var3 != null)
        {
        	if ((var3.entityHit != null && !res.canAttack(var3.entityHit)) || (var3.entityHit == null && !res.canInteract(var3.blockX, var3.blockY, var3.blockZ)))
        	{
				return "Target in MyTown protected area";
        	}
        	
        	if (explosive)
        	{
        		// 4 corner check
        		int x, y, z;
        		
        		if (var3.entityHit != null)
        		{
        			x = (int)var3.entityHit.posX;
        			y = (int)var3.entityHit.posY;
        			z = (int)var3.entityHit.posZ;
        		}
        		else
        		{
        			x = (int)var3.blockX;
        			y = (int)var3.blockY;
        			z = (int)var3.blockZ;
        		}
        		
        		if (!res.canInteract(x - explosionRadius, y, z) || 
    				!res.canInteract(x + explosionRadius, y, z) ||
    				!res.canInteract(x, y, z - explosionRadius) ||
    				!res.canInteract(x, y, z + explosionRadius))
        			return "Explosion would hit a protected town";
        	}
        }
        
        return null;
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) 
	{
	}

	@Override
	public EnumSet<TickType> ticks() 
	{
		return EnumSet.of(TickType.WORLD);
	}
	
	@Override
	public String getLabel() 
	{
		return "MyTown protection event handler";
	}
}
