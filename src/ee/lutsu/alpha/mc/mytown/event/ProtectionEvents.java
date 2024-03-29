package ee.lutsu.alpha.mc.mytown.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.entities.ItemIdRange;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.event.prot.*;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.INpc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class ProtectionEvents implements ITickHandler
{
	public static ProtectionEvents instance = new ProtectionEvents();
	public Resident lastOwner = null;
	public boolean enabled = false;
	public ArrayList<Entity> toRemove = new ArrayList<Entity>();
	public ArrayList<TileEntity> toRemoveTile = new ArrayList<TileEntity>();
	public boolean loaded = false;
	private List<Class> npcClasses = null;
	public boolean dynamicEnabling = true;
	
	public static ProtBase[] entityProtections = new ProtBase[]
	{
		Creeper.instance,
		Mobs.instance,
		TNT.instance,
		ThaumCraft.instance,
		PortalGun.instance,
		IndustrialCraft.instance,
		SteveCarts.instance,
		RailCraft.instance,
		TrainCraft.instance,
		Mekanism.instance,
		ModularPowersuits.instance
	};
	
	public static ProtBase[] tileProtections = new ProtBase[]
	{
		BuildCraft.instance,
		RedPower.instance,
		ComputerCraft.instance,
		ThaumCraft.instance
	};
	
	public static ProtBase[] toolProtections = new ProtBase[]
	{
		SingleBlockTools.instance,
		RangedTools.instance,
		ThaumCraft.instance
	};
	
	public boolean itemUsed(Resident r)
	{
		try
		{
			String kill = null;
			
			ItemStack item = r.onlinePlayer.getHeldItem();
			if (item == null)
				return true;
			
			Item tool = item.getItem();
			if (tool == null)
				return true;
			
			// Always allow the usage of cart type items
			if (ItemIdRange.contains(MyTown.instance.carts, item))
				return true;
			
			//Log.info(String.format("Item click : %s %s %s", r.name(), item, tool.getClass()));
			
			ProtBase lastCheck = null;
			kill = null;
			for (ProtBase prot : toolProtections)
			{
				if (prot.enabled && prot.isEntityInstance(tool))
				{
					lastCheck = prot;
					kill = prot.update(r, tool, item);
					if (kill != null)
						break;
				}
			}
			
			if (kill != null)
			{
				String sTool = String.format("[%s] %s", item.itemID + (item.isStackable() && item.getItemDamage() > 0 ? ":" + item.getItemDamage() : ""), tool.getLocalizedName(null));
				
				EntityPlayer pl = r.onlinePlayer;
				Log.severe(String.format("[%s]Player %s tried to bypass at dim %d, %d,%d,%d using %s - %s", lastCheck.getClass().getSimpleName(), pl.username, pl.dimension, (int)pl.posX, (int)pl.posY, (int)pl.posZ, sTool, kill));
				pl.sendChatToPlayer("§4You cannot use that here - " + kill);
				return false;
			}
		}
		catch (Exception er)
		{
			Log.severe("Error in player " + r.onlinePlayer.toString() + " item use check", er);
		}
		return true;
	}

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) 
	{
		if (!enabled)
			return;
		
		setFields();
		
		World world = (World)tickData[0];
		Entity e = null;
		TileEntity t = null;
		String kill = null;

		toRemove.clear();
		toRemoveTile.clear();
		
		try
		{
			for (int i = 0; i < world.loadedEntityList.size(); i++)
			{
				e = (Entity)world.loadedEntityList.get(i);
				if (e == null || e.isDead)
					continue;
				
				lastOwner = null;
				kill = null;
				
				if (e instanceof EntityPlayer)
				{
					EntityPlayer pl = (EntityPlayer)e;
					if (pl.isUsingItem())
					{
						Resident r = MyTownDatasource.instance.getOrMakeResident(pl);
						if (!ProtectionEvents.instance.itemUsed(r))
							r.onlinePlayer.stopUsingItem();
					}
				}
				
				for (ProtBase prot : entityProtections)
				{
					if (prot.enabled && prot.isEntityInstance(e))
					{
						kill = prot.update(e);
						if (kill != null)
							break;
					}
				}
				
				if (kill != null)
				{
					if (lastOwner != null)
					{
						if (lastOwner.isOnline())
						{
							Log.severe(String.format("Player %s tried to bypass at dim %d, %d,%d,%d using %s - %s", lastOwner.name(), lastOwner.onlinePlayer.dimension, (int)lastOwner.onlinePlayer.posX, (int)lastOwner.onlinePlayer.posY, (int)lastOwner.onlinePlayer.posZ, e.toString(), kill));
							lastOwner.onlinePlayer.sendChatToPlayer("§4You cannot use that here - " + kill);
						}
						else
							Log.severe(String.format("Player %s tried to bypass using %s - %s", lastOwner.name(), e.toString(), kill));
					}
					else
						Log.severe(String.format("Entity %s tried to bypass using %s", e.toString(), kill));
					
					toRemove.add(e);
				}
			}
			
			e = null;
			
			for (Entity en : toRemove)
				world.removeEntity(en);
			
			for (int i = 0; i < world.loadedTileEntityList.size(); i++)
			{
				t = (TileEntity)world.loadedTileEntityList.get(i);
				if (t == null)
					continue;
				
				lastOwner = null;
				kill = null;
				
				for (ProtBase prot : tileProtections)
				{
					if (prot.enabled && prot.isEntityInstance(t))
					{
						kill = prot.update(t);
						if (kill != null)
							break;
					}
				}
				
				if (kill != null)
				{
					String block = String.format("TileEntity %s @ dim %s, %s,%s,%s", t.getClass().toString(), t.worldObj.provider.dimensionId, t.xCoord, t.yCoord, t.zCoord);
					if (lastOwner != null)
					{
						if (lastOwner.isOnline())
						{
							Log.severe(String.format("Player %s tried to bypass at dim %d, %d,%d,%d using %s - %s", lastOwner.name(), lastOwner.onlinePlayer.dimension, (int)lastOwner.onlinePlayer.posX, (int)lastOwner.onlinePlayer.posY, (int)lastOwner.onlinePlayer.posZ, block, kill));
							lastOwner.onlinePlayer.sendChatToPlayer("§4You cannot use that here - " + kill);
						}
						else
							Log.severe(String.format("Player %s tried to bypass using %s - %s", lastOwner.name(), block, kill));
					}
					else
						Log.severe(String.format("TileEntity %s tried to bypass using %s", block, kill));
					
					toRemoveTile.add(t);
				}
			}
			
			for (TileEntity en : toRemoveTile)
			{
				Block.blocksList[en.worldObj.getBlockId(en.xCoord, en.yCoord, en.zCoord)]
						.dropBlockAsItem(en.worldObj, en.xCoord, en.yCoord, en.zCoord, en.worldObj.getBlockMetadata(en.xCoord, en.yCoord, en.zCoord), 0);
	            en.worldObj.setBlock(en.xCoord, en.yCoord, en.zCoord, 0);
			}
		}
		catch (Exception er)
		{
			String ms = e == null ? t == null ? "#unknown#" : t.toString() : e.toString();
			Log.severe("Error in entity " + ms + " pre-update check", er);
		}
	}
	
	public List<Class> getNPCClasses()
	{
		if (npcClasses == null)
		{
			npcClasses = Lists.newArrayList((Class)INpc.class);
			
			try
			{
				CustomNPCs.addNPCClasses(npcClasses);
			}
			catch (Throwable t)
			{
				
			}
		}
		
		return npcClasses;
	}
	
	public static List<ProtBase> getProtections()
	{
		Set<ProtBase> set = Sets.newHashSet();
		
		set.addAll(Arrays.asList(entityProtections));
		set.addAll(Arrays.asList(tileProtections));
		set.addAll(Arrays.asList(toolProtections));
		
		return new ArrayList<ProtBase>(set);
	}
	
	private void setFields()
	{
		if (loaded)
			return;

		for (ProtBase prot : getProtections())
		{
			if (dynamicEnabling)
				prot.enabled = true;
			
			if (prot.enabled && !prot.loaded())
			{
				try
				{
					prot.load();
				}
				catch (Exception e)
				{
					prot.enabled = false;
					Log.info("§f[§1Prot§f]Module %s §4failed §fto load.", prot.getClass().getSimpleName());
					
					if (!dynamicEnabling)
						throw new RuntimeException("ProtectionEvents cannot load " + prot.getClass().getSimpleName() + " class. Is " + prot.getMod() + " loaded?", e);
				}
			}
			
			if (dynamicEnabling && prot.enabled) // some are already loaded()
				Log.info("§f[§1Prot§f]Module %s §2loaded§f.", prot.getClass().getSimpleName());
		}
		
		loaded = true;
	}
	
	public void reload()
	{
		loaded = false;
		
        for (ProtBase prot : getProtections())
            prot.reload();
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
