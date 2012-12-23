package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import net.minecraft.entity.Entity;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;

public class PortalGun extends ProtBase
{
	public static PortalGun instance = new PortalGun();
	
	Class clPortalBall = null;
	
	@Override
	public void load() throws Exception
	{
		clPortalBall = Class.forName("portalgun.common.entity.EntityPortalBall");
	}
	
	@Override
	public boolean loaded() { return clPortalBall != null; }
	@Override
	public boolean isEntityInstance(Entity e) { return e.getClass() == clPortalBall; }

	@Override
	public String update(Entity e) throws Exception
	{
		if ((int)e.posX == (int)e.prevPosX && (int)e.posY == (int)e.prevPosY && (int)e.posZ == (int)e.prevPosZ) // didn't move
			return null;
		
		String owner = e.getDataWatcher().getWatchableObjectString(18);
		
		if (owner != null && !owner.equals("def")) // not default portal
		{
			Resident r = ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getOrMakeResident(owner);
		    
			for (int ii = 0; ii < 5; ii++)
			{
			    int x = (int)(e.motionX / 5.0D * ii + e.posX);
			    int y = (int)(e.motionY / 5.0D * ii + e.posY);
			    int z = (int)(e.motionZ / 5.0D * ii + e.posZ);

			    if (!r.canInteract(x, y, z, Permissions.Build))
			    	return "Cannot shoot portals in this town";
			}
		}

		return null;
	}
	
	@Override
	public String getMod() 
	{
		return "PortalgunMod";
	}
}
