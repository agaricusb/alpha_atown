package ee.lutsu.alpha.mc.mytown.event;

import ee.lutsu.alpha.mc.mytown.entities.Resident;
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
}
