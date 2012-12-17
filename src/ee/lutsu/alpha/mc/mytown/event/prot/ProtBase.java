package ee.lutsu.alpha.mc.mytown.event.prot;

import net.minecraft.src.Entity;

public abstract class ProtBase 
{
	public boolean enabled = false;
	
	public void load() throws Exception { }
	public boolean loaded() { return true; }
	public boolean isEntityInstance(Entity e) 
	{ 
		return false; 
	}
	
	public String update(Entity e) throws Exception 
	{
		return null; 
	}
	
	public abstract String getMod();
}
