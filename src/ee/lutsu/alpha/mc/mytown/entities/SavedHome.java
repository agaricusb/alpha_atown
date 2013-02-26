package ee.lutsu.alpha.mc.mytown.entities;

import net.minecraft.entity.Entity;

public class SavedHome
{
	public String name;
	
	public int dim;
	
	public double x;
	public double y;
	public double z;
	
	public float look1;
	public float look2;
	
	protected SavedHome()
	{
	}
	
	public SavedHome(String name, Entity entityFrom)
	{
		this.name = name;
		reset(entityFrom);
	}
	
	public void reset(Entity entityFrom)
	{
		dim = entityFrom.dimension;
		x = entityFrom.posX;
		y = entityFrom.posY;
		z = entityFrom.posZ;
		look1 = entityFrom.rotationYaw;
		look2 = entityFrom.rotationPitch;
	}
	
	public static SavedHome deserialize(String str)
	{
		SavedHome h = new SavedHome();
		
		String[] a = str.split("/");
		
		h.name = a[0];
		h.dim = Integer.parseInt(a[1]);
		
		h.x = Double.parseDouble(a[2]);
		h.y = Double.parseDouble(a[3]);
		h.z = Double.parseDouble(a[4]);
		
		h.look1 = Float.parseFloat(a[5]);
		h.look2 = Float.parseFloat(a[6]);
		
		return h;
	}
	
	public String serialize()
	{
		return String.format("%s/%s/%s/%s/%s/%s/%s", name, dim, x, y, z, look1, look2);
	}
}
