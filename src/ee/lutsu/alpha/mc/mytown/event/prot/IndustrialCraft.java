package ee.lutsu.alpha.mc.mytown.event.prot;

import java.lang.reflect.Field;

import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Utils;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.Permissions;
import ee.lutsu.alpha.mc.mytown.event.ProtBase;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;

public class IndustrialCraft extends ProtBase
{
	public static IndustrialCraft instance = new IndustrialCraft();

    Class clDynamite = null, clStickyDynamite, clNuke, clITNT, clEntityIC2Explosive;
    Field fFuse1, fFuse2;
    
	@Override
	public void load() throws Exception
	{
		clDynamite = Class.forName("ic2.core.block.EntityDynamite");
		clStickyDynamite = Class.forName("ic2.core.block.EntityStickyDynamite");
		clNuke = Class.forName("ic2.core.block.EntityNuke");
		clITNT = Class.forName("ic2.core.block.EntityItnt");
		clEntityIC2Explosive = Class.forName("ic2.core.block.EntityIC2Explosive");
		
		fFuse1 = clEntityIC2Explosive.getDeclaredField("fuse");
		fFuse2 = clDynamite.getDeclaredField("fuse");
	}
	
	@Override
	public boolean loaded() { return clDynamite != null; }
	@Override
	public boolean isEntityInstance(Entity e) 
	{
		Class c = e.getClass();
		return c == clDynamite || c == clStickyDynamite || c == clNuke || c == clITNT;
	}

	@Override
	public String update(Entity e) throws Exception
	{
		if (e.isDead)
			return null;
		
		Class c = e.getClass();
		int radius = 6;
		int fuse = 0;
		
		if (c == clDynamite || c == clStickyDynamite)
		{
			fuse = fFuse2.getInt(e);
			radius = 1;
		}
		else
		{
			fuse = fFuse1.getInt(e);
			radius = c == clNuke ? 35 : 6;
		}
		
		if (fuse > 1)
			return null;

        radius = radius + 2; // 2 for safety

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
	
	public String getMod() { return "IndustrialCraft"; }
	public String getComment() { return "Town permission: disableTNT"; }
}
