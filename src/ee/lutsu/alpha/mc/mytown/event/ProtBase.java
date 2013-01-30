package ee.lutsu.alpha.mc.mytown.event;

import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public abstract class ProtBase 
{
	public boolean enabled = false;
	
	public void reload() { }
	public void load() throws Exception { }
	public boolean loaded() { return true; }
	public boolean isEntityInstance(Item item) 
	{ 
		return false; 
	}
	
	public boolean isEntityInstance(Entity e) 
	{ 
		return false; 
	}
	
	public boolean isEntityInstance(TileEntity e) 
	{ 
		return false; 
	}
	
	public String update(Resident r, Item tool, ItemStack item) throws Exception 
	{
		throw new Exception("Protection doesn't support Players");
	}
	
	public String update(Entity e) throws Exception 
	{
		throw new Exception("Protection doesn't support Entity's");
	}
	
	public String update(TileEntity e) throws Exception 
	{
		throw new Exception("Protection doesn't support TileEntity's");
	}
	
	public abstract String getMod();
	public abstract String getComment();
	public boolean defaultEnabled() { return false; }
	
	public static Resident getActorFromLocation(int dim, int x, int y, int z, String defaultActor)
	{
		TownBlock block = MyTownDatasource.instance.getPermBlockAtCoord(dim, x, y, z);
		
		Resident actor = null;
		if (block != null && block.town() != null)
		{
			if (block.owner() != null)
				actor = block.owner();
			else
				actor = block.town().getFirstMayor();
		}
		
		if (actor == null) // zero resident town or in the wild
			actor = MyTownDatasource.instance.getOrMakeResident(defaultActor);
		
		return actor;
	}
}
