package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;

import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;
import ee.lutsu.alpha.mc.mytown.event.ProtectionEvents;

public class BuildCraft extends ProtBase
{
	public static BuildCraft instance = new BuildCraft();
	public List<TileEntity> checkedEntitys = new ArrayList<TileEntity>();
	public int tickRun = 0;
	public boolean tickRunDone = false;

	Class clQuarry = null, clFiller, clBuilder, clBox;
	Field fBoxQ, fBoxF, fBoxB, fmx, fmy, fmz, fxx, fxy, fxz, fBoxInit, fQuarryOwner, fQuarryBuilderDone;
	
	public void reload()
	{
		checkedEntitys.clear();
		tickRunDone = false;
		tickRun = 0;
	}
	
	@Override
	public void load() throws Exception
	{
		clQuarry = Class.forName("buildcraft.factory.TileQuarry");
		clFiller = Class.forName("buildcraft.builders.TileFiller");
		clBuilder = Class.forName("buildcraft.builders.TileBuilder");
		
		clBox = Class.forName("buildcraft.core.Box");
		
		fBoxQ = clQuarry.getField("box");
		fQuarryOwner = clQuarry.getField("placedBy");
		fQuarryBuilderDone = clQuarry.getField("builderDone");
		fBoxF = clFiller.getField("box");
		fBoxB = clBuilder.getField("box");
		
		fmx = clBox.getField("xMin");
		fmy = clBox.getField("yMin");
		fmz = clBox.getField("zMin");
		fxx = clBox.getField("xMax");
		fxy = clBox.getField("yMax");
		fxz = clBox.getField("zMax");
		fBoxInit = clBox.getField("initialized");
		
		tickRunDone = false;
		tickRun = 0;
	}
	
	@Override
	public boolean loaded() 
	{ 
		if (!tickRunDone)
		{
			if (tickRun > (MinecraftServer.getServer().worldServers.length * 20 * 60)) // skip for 1 minute
				tickRunDone = true;
			
			tickRun++;
		}
		
		return clBuilder != null;
	}
	
	@Override
	public boolean isEntityInstance(TileEntity e) 
	{ 
		Class c = e.getClass();
		
		return c == clQuarry || c == clFiller || c == clBuilder;
	}
	
	@Override
	public String update(TileEntity e) throws Exception
	{
		if (!tickRunDone)
			return null;

		if (checkedEntitys.contains(e))
			return null;
		
		String s = updateSub(e);
		
		if (s == null) // no need to check twice if it already passed
			checkedEntitys.add(e);
		
		return s == "-" ? null : s; // "-" used to bypass caching
	}

	private String updateSub(TileEntity e) throws Exception
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
			return "-";
		
		if (clazz == clQuarry && fQuarryBuilderDone.getBoolean(e)) // only check quarrys in builder mode
			return null;
		
		int ax = fmx.getInt(box);
		int ay = fmy.getInt(box);
		int az = fmz.getInt(box);
		
		int bx = fxx.getInt(box);
		int by = fxy.getInt(box);
		int bz = fxz.getInt(box);
		
		int fx = ChunkCoord.getCoord(ax);
		int fz = ChunkCoord.getCoord(az);
		int tx = ChunkCoord.getCoord(bx);
		int tz = ChunkCoord.getCoord(bz);
		
		for (int z = fz; z <= tz; z++)
		{
			for (int x = fx; x <= tx; x++)
			{
				TownBlock block = MyTownDatasource.instance.getBlock(e.worldObj.provider.dimensionId, x, z);
			
				if ((block != null && block.town() != null && !block.settings.allowBuildcraftMiners) || ((block == null || block.town() == null) && !MyTown.instance.getWorldWildSettings(e.worldObj.provider.dimensionId).allowBuildcraftMiners))
				{
					String b = block == null || block.town() == null ? "wild" : block.town().name() + (block.owner() != null ? " owned by " + block.ownerDisplay() : "");
					b = String.format("%s @ dim %s (%s,%s)", b, e.worldObj.provider.dimensionId, x, z);
					
					if (clazz == clQuarry)
					{
						EntityPlayer pl = (EntityPlayer)fQuarryOwner.get(e);
						if (pl != null)
						{
							ProtectionEvents.instance.lastOwner = MyTownDatasource.instance.getOrMakeResident(pl);
						}
						else
						{
							Log.severe(String.format("ERROR: Quarry %s,%s,%s,%s would've been popped - %s. Not popping because the placer is unknown.", e.worldObj.provider.dimensionId, e.xCoord, e.yCoord, e.zCoord, b));
							return null; // some fluke, the quarry had to be here before
						}
					}
					

					return "Region will hit " + b + " which doesn't allow buildcraft block breakers";
				}
			}
		}
		
		return null;
	}

	public String getMod() { return "BuildCraft"; }
	public String getComment() { return "Town permission: allowBuildcraftMiners"; }
}
