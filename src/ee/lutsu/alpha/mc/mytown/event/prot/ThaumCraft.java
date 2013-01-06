package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ThaumCraft extends ProtBase
{
	public static ThaumCraft instance = new ThaumCraft();
	public int explosionRadius = 6;
	
	private Class clAlumentum = null, clItemWandExcavation, clUtils;
	private Method mGetTargetBlock;

	@Override
	public void load() throws Exception
	{
		clAlumentum = Class.forName("thaumcraft.common.entities.EntityAlumentum");
		clItemWandExcavation = Class.forName("thaumcraft.common.items.wands.ItemWandExcavation");
		clUtils = Class.forName("thaumcraft.common.Utils");
		mGetTargetBlock = clUtils.getDeclaredMethod("getTargetBlock", World.class, EntityPlayer.class, boolean.class);
	}
	
	@Override
	public boolean loaded() { return clAlumentum != null; }
	@Override
	public boolean isEntityInstance(Entity e) { return e.getClass() == clAlumentum || e instanceof EntityPlayer; }
	
	@Override
	public String update(Entity e) throws Exception
	{
		if (e.getClass() == clAlumentum)
		{
			EntityThrowable t = (EntityThrowable)e;
			EntityLiving owner = t.func_85052_h();
			
			if (owner == null || !(owner instanceof EntityPlayer))
				return "No owner or is not a player";
			
			Resident thrower = ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getResident((EntityPlayer)owner);
	
			int x = (int) (t.posX + t.motionX);
			int y = (int) (t.posY + t.motionY);
			int z = (int) (t.posZ + t.motionZ);
			
			if (!thrower.canInteract(x - explosionRadius, y, z - explosionRadius, Permissions.Build) || 
				!thrower.canInteract(x - explosionRadius, y, z + explosionRadius, Permissions.Build) ||
				!thrower.canInteract(x + explosionRadius, y, z - explosionRadius, Permissions.Build) ||
				!thrower.canInteract(x + explosionRadius, y, z + explosionRadius, Permissions.Build))
				return "Explosion would hit a protected town";
		}
		else if (e instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer)e;
			if (p.getHeldItem() != null && p.isUsingItem() && p.getHeldItem().getItem() != null && p.getHeldItem().getItem().getClass() == clItemWandExcavation)
			{
				MovingObjectPosition pos = getTargetBlock(p.worldObj, p, false);
				
				if (pos != null && pos.typeOfHit == EnumMovingObjectType.TILE)
				{
					Resident res = ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getResident(p);
					
					if (!res.canInteract(pos.blockX, pos.blockY, pos.blockZ, Permissions.Build))
					{
						p.stopUsingItem();
						p.sendChatToPlayer(Term.ErrPermCannotBuildHere.toString());
					}
				}
			}
		}
		
		return null;
	}

	public MovingObjectPosition getTargetBlock(World world, EntityPlayer player, boolean par3)
	{
		try 
		{
			return (MovingObjectPosition)mGetTargetBlock.invoke(null, world, player, par3);
		}
		catch (Throwable e) 
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getMod() { return "ThaumCraft"; }
}
