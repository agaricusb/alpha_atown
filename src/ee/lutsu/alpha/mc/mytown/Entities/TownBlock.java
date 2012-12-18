package ee.lutsu.alpha.mc.mytown.Entities;

import java.security.acl.Owner;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes.Name;

import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection.Permissions;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Entity;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

public class TownBlock
{
	private World linkedWorld;
	private int world_dimension;
	private int chunkX;
	private int chunkZ;
	private Town town;
	private Resident owner;
	public String owner_name; // only for sql loading. Don't use.

	public int x() { return chunkX; }
	public int z() { return chunkZ; }
	public int worldDimension() { return world_dimension; }
	public World world() { return linkedWorld; }
	
	public Town town() { return town; }
	public Resident owner() { return owner; }
	public String ownerDisplay() { return owner == null ? "-" : owner.name(); }
	public void setTown(Town val) 
	{ 
		town = val;
		settings.setParent(town == null ? null : owner != null ? owner.settings : town.settings);
	}
	public void setOwner(Resident val) 
	{ 
		sqlSetOwner(val);
		save(); 
	}
	public void sqlSetOwner(Resident val) 
	{
		owner = val; 
		settings.setParent(town == null ? null : owner != null ? owner.settings : town.settings);
	}
	
	// extra
	public TownSettingCollection settings = new TownSettingCollection();
	
	public TownBlock(int pWorld, int x, int z)
	{
		world_dimension = pWorld;
		chunkX = x;
		chunkZ = z;
		
		linkedWorld = MinecraftServer.getServer().worldServerForDimension(pWorld);
	}
	
	public static TownBlock deserialize(String info)
	{
		String[] splits = info.split(";");
		if (splits.length < 3)
			throw new RuntimeException("Error in block info : " + info);

		TownBlock t = new TownBlock(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]), Integer.parseInt(splits[2]));
		
		if (splits.length > 3)
		{
			t.owner_name = splits[3];
			t.settings.deserialize(splits[4]);
		}
		
		return t;
	}
	
	public String serialize() // don't use space
	{
		return worldDimension() + ";" +
			String.valueOf(x()) + ";" +
			String.valueOf(z()) + ";" +
			(owner == null ? "" : owner.name()) + ";" +
			settings.serialize();
			
	}
	
	public boolean equals(TownBlock block)
	{
		return chunkX == block.chunkX && 
			   chunkZ == block.chunkZ && 
			   linkedWorld != null &&
			   linkedWorld == block.linkedWorld;
	}
	
	public boolean equals(int dim, int x, int z)
	{
		return chunkX == x && 
			   chunkZ == z && 
			   linkedWorld != null &&
			   world_dimension == dim;
	}
	
	public int squaredDistanceTo(TownBlock b)
	{
		if (world_dimension != b.world_dimension)
			throw new RuntimeException("Cannot measure distance to ");
		
		return Math.abs((chunkX - b.chunkX) * (chunkX - b.chunkX) + (chunkZ - b.chunkZ) * (chunkZ - b.chunkZ));
	}
	
	public void save()
	{
		if (town != null)
			town.save();
	}
}
