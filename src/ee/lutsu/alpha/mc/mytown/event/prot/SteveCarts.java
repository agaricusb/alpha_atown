package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityMinecart;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Vec3;

public class SteveCarts 
{
	public static SteveCarts instance = new SteveCarts();
	
	Class clSteveCart = null, clSteveModule, clRailer;
	Method mIsValidForTrack, mGetNextblock;
	Field fWorkModules, fCargo;
	
	public void load() throws Exception
	{
		clSteveCart = Class.forName("vswe.stevescarts.entMCBase");
		fWorkModules = clSteveCart.getDeclaredField("workModules");
		
		clSteveModule = Class.forName("vswe.stevescarts.baseModule");
		fCargo = clSteveModule.getDeclaredField("cargo");
		
		Class c = Class.forName("vswe.stevescarts.workModuleBase");
		mIsValidForTrack = c.getDeclaredMethod("isValidForTrack", int.class, int.class, int.class, boolean.class);
		mGetNextblock = c.getDeclaredMethod("getNextblock");

		clRailer = Class.forName("vswe.stevescarts.workModuleRailer");
	}
	
	public boolean loaded() { return clSteveCart != null; }
	public boolean isSteveEntity(Entity e) { return e.getClass() == clSteveCart; }
	
	/**
	 * Doesn't work. The classes contain links to client-side-only classes and there for mapping to them will crash us.
	 * Even by adding client to the server, FMLRelauncher wont allow us to load client base class because it has the side.client tag on it.
	 */
	public String update(Entity e) throws Exception
	{
		if ((int)e.posX == (int)e.prevPosX && (int)e.posZ == (int)e.prevPosZ) // didn't move
			return null;
		
		fWorkModules.setAccessible(true);
		fCargo.setAccessible(true);
		mIsValidForTrack.setAccessible(true);
		
		ArrayList modules = (ArrayList)fWorkModules.get(e);
		ArrayList<Object> railerModules = new ArrayList<Object>();
		
		for (Object o : modules)
		{
			if (!clRailer.isInstance(o))
				continue;
			
			railerModules.add(o);
		}
		
		if (railerModules.size() < 1) // no railer
			return null;
		
		Object module = railerModules.get(0);
		Vec3 next = (Vec3)mGetNextblock.invoke(module);
		
		Log.log("next: " + next.toString());
		
		if (!tryWorkRailer(module, next)) // wont place a rail
			return null;
		
		Log.log("Going on");
		
		TownBlock b = MyTownDatasource.instance.getBlock(e.dimension, ((int)next.xCoord) >> 4, ((int)next.zCoord) >> 4);
		if (b == null && Town.canPluginChangeWild("StevesCarts", e))
			return null;
		
		if (b != null && b.canPluginChange("StevesCarts", e))
			return null;
		
		Log.log("Blocking");

		boolean hasRails = false;
		for (Object railer : railerModules)
		{
			ItemStack[] cargo = (ItemStack[])fCargo.get(railer);
			
			for (int i = 0; i < cargo.length; i++)
			{
				ItemStack stack = cargo[i];
				if (stack == null)
					continue;
				
				hasRails = true;
				cargo[i] = null;
				e.entityDropItem(stack, 1);
			}
		}
		
		if (hasRails)
			Log.log(String.format("§4A railer steve cart found in %s at dim %s, %s,%s,%s. Dropping rails.",
					b == null || b.town() == null ? "wilderness" : b.town().name(),
					e.dimension, (int)next.xCoord, (int)next.yCoord, (int)next.zCoord));
		
		return null;
	}
	
	private boolean tryWorkRailer(Object cart, Vec3 next) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
	    int x = (int)next.xCoord;
	    int y = (int)next.yCoord;
	    int z = (int)next.zCoord;
	    
		return canPlaceTrack(cart, x, y + 1, z) || canPlaceTrack(cart, x, y, z) || canPlaceTrack(cart, x, y - 1, z);
	}

	private boolean canPlaceTrack(Object cart, int i, int j, int k) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return (Boolean)mIsValidForTrack.invoke(cart, i, j, k, true);
	}
}
