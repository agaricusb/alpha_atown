package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;

public class MiningLaser extends ProtBase
{
	public static MiningLaser instance = new MiningLaser();
	
	public int explosionRadius = 6;
	Class clLaser = null;
	Field fTickInAir, fOwner, fExplosive;
	
	@Override
	public void load() throws Exception
	{
		clLaser = Class.forName("ic2.core.item.tool.EntityMiningLaser");
		fOwner = clLaser.getDeclaredField("owner");
		fTickInAir = clLaser.getDeclaredField("ticksInAir");
		fExplosive = clLaser.getDeclaredField("explosive");
	}
	
	@Override
	public boolean loaded() { return clLaser != null; }
	@Override
	public boolean isEntityInstance(Entity e) { return e.getClass() == clLaser; }

	@Override
	public String update(Entity e) throws Exception
	{
		if ((int)e.posX == (int)e.prevPosX && (int)e.posY == (int)e.prevPosY && (int)e.posZ == (int)e.prevPosZ) // didn't move
			return null;
		
		fTickInAir.setAccessible(true);
		EntityPlayer owner = (EntityPlayer)fOwner.get(e); // actually living
		Integer ticksInAir = (Integer)fTickInAir.get(e);
		Boolean explosive = (Boolean)fExplosive.get(e);
		
		if (owner == null)
			return "no owner";

		Resident res = ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getOrMakeResident(owner);
		
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
        	if ((var3.entityHit != null && !res.canAttack(var3.entityHit)) || (var3.entityHit == null && !res.canInteract(var3.blockX, var3.blockY, var3.blockZ, Permissions.Build)))
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
        		
        		if (!res.canInteract(x - explosionRadius, y, z - explosionRadius, Permissions.Build) || 
    				!res.canInteract(x - explosionRadius, y, z + explosionRadius, Permissions.Build) ||
    				!res.canInteract(x + explosionRadius, y, z - explosionRadius, Permissions.Build) ||
    				!res.canInteract(x + explosionRadius, y, z + explosionRadius, Permissions.Build))
        			return "Explosion would hit a protected town";
        	}
        }
        
        return null;
	}

	public String getMod() { return "IndustrialCraft2"; }
	public String getComment() { return "Build & PVP check: EntityMiningLaser"; }
}
