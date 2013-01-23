package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Utils;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ThaumCraft extends ProtBase
{
	public static ThaumCraft instance = new ThaumCraft();
	public int explosionRadius = 6;
	
	private Class clAlumentum = null, clItemWandExcavation;

	@Override
	public void load() throws Exception
	{
		clAlumentum = Class.forName("thaumcraft.common.entities.projectile.EntityAlumentum");
		clItemWandExcavation = Class.forName("thaumcraft.common.items.wands.ItemWandExcavation");
	}
	
	@Override
	public boolean loaded() { return clAlumentum != null; }
	@Override
	public boolean isEntityInstance(Entity e) { return e.getClass() == clAlumentum; }
	@Override
	public boolean isEntityInstance(Item e) { return e.getClass() == clItemWandExcavation; }
	
	@Override
	public String update(Entity e) throws Exception
	{
		EntityThrowable t = (EntityThrowable)e;
		EntityLiving owner = t.getThrower();
		
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

		return null;
	}
	
	@Override
	public String update(Resident res, Item tool, ItemStack item) throws Exception
	{
		MovingObjectPosition pos = Utils.getMovingObjectPositionFromPlayer(res.onlinePlayer.worldObj, res.onlinePlayer, false, 10.0D);
		
		if (pos != null && pos.typeOfHit == EnumMovingObjectType.TILE)
		{
			if (!res.canInteract(pos.blockX, pos.blockY, pos.blockZ, Permissions.Build))
				return "Cannot build here";
		}
		
		return null;
	}
	
	public String getMod() { return "ThaumCraft"; }
	public String getComment() { return "Build check: EntityAlumentum & ItemWandExcavation"; }
}
