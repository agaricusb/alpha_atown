package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;

import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;

public class Mobs extends ProtBase
{
	public static Mobs instance = new Mobs();
	public String getMod() { return "Vanilla-Mobs"; }

	@Override
	public void load() throws Exception
	{
	}
	
	@Override
	public boolean loaded() { return true; }
	@Override
	public boolean isEntityInstance(Entity e) { return e instanceof EntityMob; }
	
	@Override
	public String update(Entity e) throws Exception
	{
		EntityMob mob = (EntityMob)e;
	
        if (e.isEntityAlive())
        {
        	if (!canBe(mob))
        		return "Entered mob-free zone";
        }
        
		return null;
	}
	
	private boolean canBe(EntityMob mob)
	{
		TownBlock b = MyTownDatasource.instance.getBlock(mob.dimension, mob.chunkCoordX, mob.chunkCoordZ);
		if (b == null || b.town() == null)
			return true;
		
		if (!b.settings.disableMobs)
			return true;

		if (!b.settings.yCheckOn)
			return false;

		if (b.settings.yCheckFrom < mob.posY && mob.posY < b.settings.yCheckTo) // intersects
			return false;
		else
			return true;
	}
}
