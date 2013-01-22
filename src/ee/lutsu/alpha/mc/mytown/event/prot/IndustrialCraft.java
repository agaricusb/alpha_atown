package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.util.List;

import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Utils;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class IndustrialCraft extends ProtBase
{
	public static IndustrialCraft instance = new IndustrialCraft();

	// tnts
    Class clDynamite = null, clStickyDynamite, clNuke, clITNT, clEntityIC2Explosive;
    Field fFuse1, fFuse2;
    
    // laser
	public int explosionRadius = 6;
	Class clLaser = null;
	Field fTickInAir, fOwner, fExplosive;
    
	@Override
	public void load() throws Exception
	{
		clDynamite = Class.forName("ic2.core.block.EntityDynamite");
		clStickyDynamite = Class.forName("ic2.core.block.EntityStickyDynamite");
		clNuke = Class.forName("ic2.core.block.EntityNuke");
		clITNT = Class.forName("ic2.core.block.EntityItnt");
		clEntityIC2Explosive = Class.forName("ic2.core.block.EntityIC2Explosive");
		
		fFuse1 = clEntityIC2Explosive.getDeclaredField("fuse");
		fFuse2 = clDynamite.getDeclaredField("fuse");
		
		clLaser = Class.forName("ic2.core.item.tool.EntityMiningLaser");
		fOwner = clLaser.getDeclaredField("owner");
		fTickInAir = clLaser.getDeclaredField("ticksInAir");
		fExplosive = clLaser.getDeclaredField("explosive");
	}
	
	@Override
	public boolean loaded() { return clDynamite != null; }
	@Override
	public boolean isEntityInstance(Entity e) 
	{
		Class c = e.getClass();
		return c == clLaser || c == clDynamite || c == clStickyDynamite || c == clNuke || c == clITNT;
	}

	@Override
	public String update(Entity e) throws Exception
	{
		if (e.isDead)
			return null;
		
		Class c = e.getClass();
		if (c == clLaser)
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
		else
		{
			int radius = 6;
			int fuse = 0;
			
			if (c == clDynamite || c == clStickyDynamite)
			{
				fuse = fFuse2.getInt(e);
				radius = 1;
			}
			else
			{
				fuse = fFuse1.getInt(e);
				radius = c == clNuke ? 35 : 6;
			}
			
			if (fuse > 1)
				return null;
	
	        radius = radius + 2; // 2 for safety
	
	        if (canBlow(e.dimension, e.posX - radius, e.posY - radius, e.posY + radius, e.posZ - radius) &&
	        	canBlow(e.dimension, e.posX - radius, e.posY - radius, e.posY + radius, e.posZ + radius) &&
	        	canBlow(e.dimension, e.posX + radius, e.posY - radius, e.posY + radius, e.posZ - radius) &&
	        	canBlow(e.dimension, e.posX + radius, e.posY - radius, e.posY + radius, e.posZ + radius))
	        	return null;
	
	        return "TNT explosion disabled here";
		}
	}

	
	private boolean canBlow(int dim, double x, double yFrom, double yTo, double z)
	{
		TownBlock b = MyTownDatasource.instance.getBlock(dim, ChunkCoord.getCoord(x), ChunkCoord.getCoord(z));
		if (b != null && b.settings.yCheckOn)
		{
			if (yTo < b.settings.yCheckFrom || yFrom > b.settings.yCheckTo)
				b = b.getFirstFullSidingClockwise(b.town());
		}
		
		if (b == null || b.town() == null)
			return !MyTown.instance.getWorldWildSettings(dim).disableTNT;

		return !b.settings.disableTNT;
	}
	
	public String getMod() { return "IndustrialCraft2"; }
	public String getComment() { return "Town permission: disableTNT, Build & PVP check: EntityMiningLaser"; }
}
