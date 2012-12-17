package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;

import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.TileEntity;

public class BuildCraft extends ProtBase
{
	public static BuildCraft instance = new BuildCraft();

	Class clQuarry = null, clFiller, clBuilder, clBox;
	Field fBoxQ, fBoxF, fBoxB, fmx, fmy, fmz, fxx, fxy, fxz, fBoxInit, fQuarryOwner;
	
	@Override
	public void load() throws Exception
	{
		clQuarry = Class.forName("buildcraft.factory.TileQuarry");
		clFiller = Class.forName("buildcraft.builders.TileFiller");
		clBuilder = Class.forName("buildcraft.builders.TileBuilder");
		
		clBox = Class.forName("buildcraft.core.Box");
		
		fBoxQ = clQuarry.getField("box");
		fQuarryOwner = clQuarry.getField("placedBy");
		fBoxF = clFiller.getField("box");
		fBoxB = clBuilder.getField("box");
		
		fmx = clBox.getField("xMin");
		fmy = clBox.getField("yMin");
		fmz = clBox.getField("zMin");
		fxx = clBox.getField("xMax");
		fxy = clBox.getField("yMax");
		fxz = clBox.getField("zMax");
		fBoxInit = clBox.getField("initialized");
	}
	
	@Override
	public boolean loaded() { return clBuilder != null; }
	@Override
	public boolean isEntityInstance(TileEntity e) 
	{ 
		Class c = e.getClass();
		
		return c == clQuarry || c == clFiller || c == clBuilder;
	}

	@Override
	public String update(TileEntity e) throws Exception
	{
		Object box = null;
		Class clazz = e.getClass();

		if (clazz == clQuarry)
			box = fBoxQ.get(e);
		else if (clazz == clFiller)
			box = fBoxF.get(e);
		else if (clazz == clBuilder)
			box = fBoxB.get(e);
		
		boolean init = (boolean)fBoxInit.getBoolean(box);
		if (!init)
			return null;
		
		int ax = fmx.getInt(box);
		int ay = fmy.getInt(box);
		int az = fmz.getInt(box);
		
		int bx = fxx.getInt(box);
		int by = fxy.getInt(box);
		int bz = fxz.getInt(box);
		
		int fx = ax >> 4;
		int fz = az >> 4;
		int tx = bx >> 4;
		int tz = bz >> 4;
		
		for (int z = fz; z <= tz; z++)
		{
			for (int x = fx; x <= tx; x++)
			{
				TownBlock block = MyTownDatasource.instance.getBlock(e.worldObj.provider.dimensionId, x, z);
			
				if (block != null)
				{
					if (block.canPluginChange("BuildCraft", e))
						return null;
					
					if (clazz == clQuarry)
					{
						EntityPlayer pl = (EntityPlayer)fQuarryOwner.get(e);
						if (pl != null)
						{
							ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getOrMakeResident(pl);
						}
					}
					
					return "Region will hit a town block that doesn't allow this";
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String getMod() 
	{
		return "BuildCraft";
	}

}
