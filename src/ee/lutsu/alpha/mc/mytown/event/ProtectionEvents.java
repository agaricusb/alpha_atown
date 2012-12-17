package ee.lutsu.alpha.mc.mytown.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.event.prot.MiningLaser;
import ee.lutsu.alpha.mc.mytown.event.prot.PortalGun;
import ee.lutsu.alpha.mc.mytown.event.prot.ProtBase;
import ee.lutsu.alpha.mc.mytown.event.prot.SteveCarts;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class ProtectionEvents implements ITickHandler
{
	public static ProtectionEvents instance = new ProtectionEvents();
	public Resident lastOwner = null;
	public boolean enabled = false;
	
	public static ProtBase[] protections = new ProtBase[]
	{
		PortalGun.instance,
		MiningLaser.instance,
		SteveCarts.instance
	};

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) 
	{
		if (!enabled)
			return;
		
		setFields();
		
		World world = (World)tickData[0];
		Entity e = null;
		String kill = null;
		ArrayList<Entity> toRemove = new ArrayList<Entity>();
		
		try
		{
			for (int i = 0; i < world.loadedEntityList.size(); i++)
			{
				e = (Entity)world.loadedEntityList.get(i);
				lastOwner = null;
				kill = null;
				
				for (ProtBase prot : protections)
				{
					if (prot.enabled && prot.isEntityInstance(e))
					{
						kill = prot.update(e);
						break;
					}
				}
				
				if (kill != null)
				{
					String owner = lastOwner != null ? lastOwner.name() : "#unknown#";

					Log.severe(String.format("Player %s tried to bypass at dim %d, %d,%d,%d using %s - %s", lastOwner.name(), lastOwner.onlinePlayer.dimension, (int)lastOwner.onlinePlayer.posX, (int)lastOwner.onlinePlayer.posY, (int)lastOwner.onlinePlayer.posZ, e.toString(), kill));
					
					if (lastOwner != null && lastOwner.isOnline())
						lastOwner.onlinePlayer.sendChatToPlayer("§4You cannot use that here - " + kill);
	
					toRemove.add(e);
				}
			}
			
			for (Entity en : toRemove)
				world.removeEntity(en);
		}
		catch(Exception er)
		{
			String ms = e == null ? "#unknown#" : e.toString();
			throw new RuntimeException("Error in entity " + ms + " pre-update check", er);
		}
	}
	
	private void setFields()
	{
		for (ProtBase prot : protections)
		{
			if (prot.enabled && !prot.loaded())
			{
				try
				{
					prot.load();
				}
				catch (Exception e)
				{
					throw new RuntimeException("ProtectionEvents cannot load " + prot.getClass().getSimpleName() + " class. Is " + prot.getMod() + " loaded?", e);
				}
			}
		}
	}
	

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) 
	{
	}

	@Override
	public EnumSet<TickType> ticks() 
	{
		return EnumSet.of(TickType.WORLD);
	}
	
	@Override
	public String getLabel() 
	{
		return "MyTown protection event handler";
	}
}
