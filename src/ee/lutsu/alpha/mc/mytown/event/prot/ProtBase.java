package ee.lutsu.alpha.mc.mytown.event.prot;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public abstract class ProtBase 
{
	public boolean enabled = false;
	
	public void load() throws Exception { }
	public boolean loaded() { return true; }
	public boolean isEntityInstance(Entity e) 
	{ 
		return false; 
	}
	
	public boolean isEntityInstance(TileEntity e) 
	{ 
		return false; 
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
}
