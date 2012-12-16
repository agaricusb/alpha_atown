package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes.Name;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Entity;
import net.minecraft.src.World;

public class TownBlock
{
	public enum Permissions
	{
		None, // First char has to be different
		Enter,
		Loot,
		Access,
		Build;
		
		public static Permissions parse(String str)
		{
			for (Permissions val : values())
			{
				if (val.toString().toLowerCase().startsWith(str.toLowerCase()))
					return val;
			}
			
			return None;
		}
		
		public String getShort() { return toString().substring(0, 0); }
	}
	
	private World linkedWorld;
	private int world_dimension;
	private int chunkX;
	private int chunkZ;
	private Town town;
	private Resident owner;
	public String owner_name; // only for sql loading. Don't use.
	
	private Permissions townPerm = Permissions.Loot;
	private Permissions nationPerm = Permissions.Enter;
	private Permissions outsiderPerm = Permissions.Enter;
	private Permissions friendPerm = Permissions.Build;
	
	public int x() { return chunkX; }
	public int z() { return chunkZ; }
	public int worldDimension() { return world_dimension; }
	public World world() { return linkedWorld; }
	
	public Town town() { return town; }
	public Resident owner() { return owner; }
	public void setTown(Town val) { town = val; }
	public void setOwner(Resident val) { owner = val; save(); }
	public void sqlSetOwner(Resident val) { owner = val; }
	
	// extra
	public Map<String, String> settings = genSettings();
	
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
			t.townPerm = Permissions.parse(splits[3].substring(0, 0));
			t.nationPerm = Permissions.parse(splits[3].substring(1, 1));
			t.outsiderPerm = Permissions.parse(splits[3].substring(2, 2));
			t.friendPerm = Permissions.parse(splits[3].substring(3, 3));
		}
		else
			t.townPerm = Permissions.Build;
		
		return t;
	}
	
	public String serialize() // don't use space
	{
		return worldDimension() + ";" +
			String.valueOf(x()) + ";" +
			String.valueOf(z()) + ";" +
			townPerm.getShort() + nationPerm.getShort() + outsiderPerm.getShort() + friendPerm.getShort();
			
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
	
	public boolean canPluginChange(String plugin, Entity e)
	{
		if (town == null)
			return Town.canPluginChangeWild(plugin, e);
		
		return town.canPluginChange(plugin, e);
	}
	
	public void save()
	{
		if (town != null)
			town.save();
	}
	
	public void recalcPerms()
	{
		
	}
	
	public void forcePermsToInherit()
	{
		
	}
	
	// returns town settings with all null values
	public static HashMap<String, String> genSettings()
	{
		HashMap<String, String> settings = Town.genSettings();
		
		for (Entry<String, String> kv : settings.entrySet())
		{
			settings.put(kv.getKey(), null);
		}

		return settings;
	}
}
