package ee.lutsu.alpha.mc.mytown;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import ee.lutsu.alpha.mc.mytown.entities.*;
import ee.lutsu.alpha.mc.mytown.sql.MyTownDB;

public class MyTownDatasource extends MyTownDB 
{
	public static MyTownDatasource instance = new MyTownDatasource();
	
	public HashSet<Resident> residents = new HashSet<Resident>();
	public HashSet<Town> towns = new HashSet<Town>();
	public HashSet<TownBlock> blocks = new HashSet<TownBlock>();
	public HashSet<Nation> nations = new HashSet<Nation>();
	
	public void init() throws Exception
	{
		residents = new HashSet<Resident>();
		towns = new HashSet<Town>();
		blocks = new HashSet<TownBlock>();
		nations = new HashSet<Nation>();
		
		dispose();
		connect();
		load();
		
		towns.addAll(loadTowns());
		residents.addAll(loadResidents()); // links to towns
		
		for(Town t : towns)
		{
			for(TownBlock res : t.blocks())
			{
				if (res.owner_name != null) // map block owners
				{
					Resident r = getResident(res.owner_name);
					res.sqlSetOwner(r);
					res.owner_name = null;
				}
				
				blocks.add(res); // add block to global list
			}
		}
		
		nations.addAll(loadNations());
		
		addAllOnlinePlayers();
	}
	
	public void addAllOnlinePlayers()
	{
		for(Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
		{
			EntityPlayer pl = (EntityPlayer)obj;
			getOrMakeResident(pl);
		}
	}
	
	public void addTown(Town t)
	{
		towns.add(t);
	}
	
	public void addNation(Nation n)
	{
		nations.add(n);
	}
	
	public TownBlock getOrMakeBlock(int world_dimension, int x, int z)
	{
		for (TownBlock res : blocks)
		{
			if (res.equals(world_dimension, x, z))
				return res;
		}
		
		TownBlock res = new TownBlock(world_dimension, x, z);
		blocks.add(res);
		return res;
	}
	
	public TownBlock getBlock(int world_dimension, int x, int z)
	{
		for (TownBlock res : blocks)
		{
			if (res.equals(world_dimension, x, z))
				return res;
		}

		return null;
	}
	
	public Town getTown(String name)
	{
		for (Town res : towns)
		{
			if (res.name().equalsIgnoreCase(name))
				return res;
		}

		return null;
	}
	
	@Override
	public Town getTown(int id) 
	{
		for (Town res : towns)
		{
			if (res.id() == id)
				return res;
		}

		return null;
	}
	
	public Nation getNation(String name)
	{
		for (Nation res : nations)
		{
			if (res.name().equalsIgnoreCase(name))
				return res;
		}

		return null;
	}
	
	public Resident getOrMakeResident(EntityPlayer player)
	{
		for (Resident res : residents)
		{
			if (res.onlinePlayer == player)
				return res;
		}

		Resident r = getOrMakeResident(player.getEntityName());
		r.onlinePlayer = player;
		return r;
	}
	
	public Resident getResident(EntityPlayer player)
	{
		for (Resident res : residents)
		{
			if (res.onlinePlayer == player)
				return res;
		}
		
		return null;
	}
	
	public Resident getOrMakeResident(String name) // case sensitive
	{
		for (Resident res : residents)
		{
			if (res.name().equals(name))
				return res;
		}
		
		Resident res = new Resident(name);
		residents.add(res);
		return res;
	}
	
	public Resident getResident(String name) // case in-sensitive
	{
		for (Resident res : residents)
		{
			if (res.name().equalsIgnoreCase(name))
				return res;
		}

		return null;
	}
	
	public void unloadTown(Town t)
	{
		towns.remove(t);
	}
	
	public void unloadNation(Nation n)
	{
		nations.remove(n);
	}
	
	public void unloadBlock(TownBlock b)
	{
		b.settings.setParent(null);
		blocks.remove(b);
	}
	
	public void unloadResident(Resident r)
	{
		/*
		if (r.onlinePlayer == null && r.town() == null)
			residents.remove(r);
		*/
	}
}
