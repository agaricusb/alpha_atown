package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;

import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;

public class TNT extends ProtBase
{
	public static TNT instance = new TNT();

    public int explosionRadius = 4;
    
	@Override
	public void load() throws Exception
	{
	}
	
	@Override
	public boolean loaded() { return true; }
	@Override
	public boolean isEntityInstance(Entity e) { return e instanceof EntityTNTPrimed; }
	
	@Override
	public String update(Entity e) throws Exception
	{
		EntityTNTPrimed tnt = (EntityTNTPrimed)e;
		if (tnt.isDead || tnt.fuse > 1)
			return null;

        int radius = explosionRadius + 2; // 2 for safety

        if (canBlow(e.dimension, e.posX - radius, e.posY - radius, e.posY + radius, e.posZ - radius) &&
        	canBlow(e.dimension, e.posX - radius, e.posY - radius, e.posY + radius, e.posZ + radius) &&
        	canBlow(e.dimension, e.posX + radius, e.posY - radius, e.posY + radius, e.posZ - radius) &&
        	canBlow(e.dimension, e.posX + radius, e.posY - radius, e.posY + radius, e.posZ + radius))
        	return null;

        return "TNT explosion disabled here";
	}
	
	private boolean canBlow(int dim, double x, double yFrom, double yTo, double z)
	{
		TownBlock b = MyTownDatasource.instance.getBlock(dim, ChunkCoord.getCoord(x), ChunkCoord.getCoord(z));
		if (b != null && b.settings.yCheckOn)
		{
			if (yTo < b.settings.yCheckFrom || yFrom > b.settings.yCheckTo)
				b = b.getFirstFullSidingClockwise(b.town());
		}
		
		if (b == null || b.town() == null)
			return !MyTown.instance.getWorldWildSettings(dim).disableTNT;

		return !b.settings.disableTNT;
	}
	
	public String getMod() { return "VanillaTNT"; }
	public String getComment() { return "Town permission: disableTNT"; }
	public boolean defaultEnabled() { return true; }
}
