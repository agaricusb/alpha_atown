package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.commands.CmdChat;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityMinecart;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Vec3;

public class SteveCarts extends ProtBase
{
	public static SteveCarts instance = new SteveCarts();
	
	Class clSteveCart = null, clRailer, clMiner;
	Method mGetNextblock;
	Field fWorkModules;
	
	@Override
	public void load() throws Exception
	{
		clSteveCart = Class.forName("vswe.stevescarts.entMCBase");
		fWorkModules = clSteveCart.getDeclaredField("workModules");

		Class c = Class.forName("vswe.stevescarts.workModuleBase");
		mGetNextblock = c.getDeclaredMethod("getNextblock");

		clRailer = Class.forName("vswe.stevescarts.workModuleRailer");
		clMiner = Class.forName("vswe.stevescarts.workModuleMiner");
	}
	
	@Override
	public boolean loaded() { return clSteveCart != null; }
	@Override
	public boolean isEntityInstance(Entity e) { return e.getClass() == clSteveCart; }
	
	@Override
	public String update(Entity e) throws Exception
	{
		if ((int)e.posX == (int)e.prevPosX && (int)e.posZ == (int)e.prevPosZ) // didn't move
			return null;
		
		fWorkModules.setAccessible(true);
		
		ArrayList modules = (ArrayList)fWorkModules.get(e);
		ArrayList<Object> railerModules = new ArrayList<Object>();
		ArrayList<Object> minerModules = new ArrayList<Object>();
		
		for (Object o : modules)
		{
			if (clRailer.isInstance(o))
				railerModules.add(o);
			if (clMiner.isInstance(o))
				minerModules.add(o);
		}
		
		Object module = null;
		if (railerModules.size() > 0)
			module = railerModules.get(0);
		else if (minerModules.size() > 0)
			module = minerModules.get(0);
		else
			return null; // none found
		
		Vec3 next = (Vec3)mGetNextblock.invoke(module);
		TownBlock b = MyTownDatasource.instance.getBlock(e.dimension, ((int)next.xCoord) >> 4, ((int)next.zCoord) >> 4);
		
		if (railerModules.size() > 0) // railer
		{
			if ((b == null && !Town.canPluginChangeWild("StevesCarts", "railer", e)) || (b != null && !b.canPluginChange("StevesCarts", "railer", e)))
			{
				blockAction((EntityMinecart)e, b);
				return null;
			}
		}
		
		if (minerModules.size() > 0) // miner
		{
			if ((b == null && !Town.canPluginChangeWild("StevesCarts", "miner", e)) || (b != null && !b.canPluginChange("StevesCarts", "miner", e)))
			{
				blockAction((EntityMinecart)e, b);
				return null;
			}
		}
		
		return null;
	}
	
	private void blockAction(EntityMinecart e, TownBlock b) throws IllegalArgumentException, IllegalAccessException
	{
		e.setDead();
		e.dropCartAsItem();
		
		Log.severe(String.format("§4Stopped a steve cart found in %s @ dim %s, %s,%s,%s",
				b == null || b.town() == null ? "wilderness" : b.town().name(),
				e.dimension, (int)e.posX, (int)e.posY, (int)e.posZ));
		
		String msg = String.format("A steve cart broke @ %s,%s,%s because it wasn't allowed there", (int)e.posX, (int)e.posY, (int)e.posZ);
		String formatted = Formatter.formatChatSystem(msg, "<MyTown> " + msg, ChatChannel.Local);
		CmdChat.sendChatToAround(e.dimension, e.posX, e.posY, e.posZ, formatted);
	}

	@Override
	public String getMod() 
	{
		return "StevesCarts";
	}
	/*
	 * 		//fCargo.setAccessible(true);
		//mIsValidForTrack.setAccessible(true);
	 * module = railerModules.get(0);
			// will always return false if the miner is under ground because the miner first removes the wall
			//if (tryWorkRailer(module, next))
			//{
			 * 
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
					Log.severe(String.format("§4A railer steve cart found in %s at dim %s, %s,%s,%s. Dropping rails.",
							b == null || b.town() == null ? "wilderness" : b.town().name(),
							e.dimension, (int)next.xCoord, (int)next.yCoord, (int)next.zCoord));
							
								
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
	 */
}
