package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.jar.Attributes.Name;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Entity;
import net.minecraft.src.World;

public class TownBlock
{
	private World linkedWorld;
	private int world_dimension;
	private int chunkX;
	private int chunkZ;
	private Town town;
	
	public int x() { return chunkX; }
	public int z() { return chunkZ; }
	public int worldDimension() { return world_dimension; }
	public World world() { return linkedWorld; }
	
	public Town town() { return town; }
	public void setTown(Town val) { town = val; }
	
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
		if (splits.length != 3)
			throw new RuntimeException("Error in block info : " + info);

		return new TownBlock(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]), Integer.parseInt(splits[2]));
	}
	
	public String serialize()
	{
		return worldDimension() + ";" + String.valueOf(x()) + ";" + String.valueOf(z());
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
		return Math.abs((chunkX - b.chunkX) * (chunkX - b.chunkX) + (chunkZ - b.chunkZ) * (chunkZ - b.chunkZ));
	}
	
	public boolean canPluginChange(String plugin, Entity e)
	{
		if (town == null)
			return Town.canPluginChangeWild(plugin, e);
		
		return town.canPluginChange(plugin, e);
	}
}
