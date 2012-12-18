package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;

import com.google.common.base.Joiner;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.TileEntity;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection.ISettingsSaveHandler;

public class Town 
{
	public static int perResidentBlocks = 16;
	public static int minDistanceFromOtherTown = 5;
	public static int dontSendCartNotification = 5000;
	public static boolean allowFullPvp = false;
	public static boolean allowMemberToForeignPvp = true;
	public static boolean bouncingOn = true;

	private int id;
	private String name;
	private int extraBlocks;
	private List<Resident> residents = new ArrayList<Resident>();
	private List<TownBlock> blocks;
	private Nation nation;
	
	public long minecraftNotificationTime = 0;
	
	public String name() { return name; }
	public int id() { return id; }
	public int extraBlocks() { return extraBlocks; }
	public List<Resident> residents() { return residents; }
	public List<TownBlock> blocks() { return blocks; }
	public Nation nation() { return nation; }
	
	public void setId(int val) { id = val; }
	public void setExtraBlocks(int val) { extraBlocks = val; save(); }
	public void sqlSetExtraBlocks(int val) { extraBlocks = val; }
	public void setNation(Nation n) { nation = n; } // used internally only
	
	// extra
	public boolean bounceNonMembers = false;
	public TownSettingCollection settings = new TownSettingCollection();
	
	public void setBounce(boolean val) { bounceNonMembers = val; save(); }

	public Town(String pName, Resident creator, TownBlock home) throws CommandException
	{
		if (creator.town() != null)
			throw new CommandException(Term.TownErrCreatorPartOfTown);
		
		canSetName(pName);
		
		if (home != null)
			canAddBlock(home, true);

		id = -1;
		name = pName;
		
		residents = new ArrayList<Resident>();
		blocks = new ArrayList<TownBlock>();
		
		residents.add(creator);
		creator.setTown(this);
		creator.setRank(Rank.Mayor);
		
		if (home != null)
		{
			home.setTown(this);
			blocks.add(home);
		}
		
		setSettings();
		MyTownDatasource.instance.addTown(this);
		save(); // has town id now
		creator.save();
	}
	
	/**
	 * Used by SQL loading
	 */
	public Town(int pId, String pName, int pExtraBlocks, List<TownBlock> pBlocks, String extra)
	{
		id = pId;
		name = pName;
		extraBlocks = pExtraBlocks;
		blocks = pBlocks;

		for(TownBlock res : blocks)
			res.setTown(this);

		setSettings();
		deserializeExtra(extra);
	}
	
	private void setSettings()
	{
		settings.tag = this;
		settings.setParent(MyTown.instance.serverSettings);
		settings.saveHandler = new ISettingsSaveHandler() 
		{
			public void save(TownSettingCollection sender, Object tag) 
			{
				Town r = (Town)tag;
				r.save();
			}
		};
	}
	
	public int allowedBlocksWOExtra()
	{
		return residents.size() * perResidentBlocks;
	}
	
	public int totalBlocks()
	{
		return allowedBlocksWOExtra() + extraBlocks;
	}
	
	public int freeBlocks()
	{
		return totalBlocks() - blocks.size();
	}
	
	public void setResidentRank(Resident res, Rank r)
	{
		res.setRank(r);
		res.save();
	}
	
	public void setTownName(String newName) throws CommandException
	{
		canSetName(newName);
		
		name = newName;
		save();
	}
	
	public void canSetName(String name) throws CommandException
	{
		if (name == null || name.equals(""))
			throw new CommandException(Term.TownErrTownNameCannotBeEmpty);
		
		for(Town t : MyTownDatasource.instance.towns)
		{
			if (t.name.equalsIgnoreCase(name))
				throw new CommandException(Term.TownErrTownNameAlreadyInUse);
		}
	}
	
	public void canAddBlock(TownBlock block, boolean ignoreRoomCheck) throws CommandException
	{
		if (block.town() != null)
			throw new CommandException(Term.TownErrAlreadyClaimed);
		
		int sqr = minDistanceFromOtherTown * minDistanceFromOtherTown;
		for(TownBlock b : MyTownDatasource.instance.blocks)
		{
			if (b != block && b.worldDimension() == block.worldDimension() && b.town() != this && block.squaredDistanceTo(b) <= sqr)
				throw new CommandException(Term.TownErrBlockTooCloseToAnotherTown);
		}
		
		if (!ignoreRoomCheck && freeBlocks() < 1)
			throw new CommandException(Term.TownErrNoFreeBlocks);
	}
	
	public void addBlock(TownBlock block) throws CommandException
	{
		canAddBlock(block, false);
		
		block.setTown(this);
		blocks.add(block);
		save();
	}
	
	public void addResident(Resident res) throws CommandException
	{
		if (res.town() != null)
			throw new CommandException(Term.TownErrPlayerAlreadyInTown);
		
		res.setTown(this);
		residents.add(res);
		res.save();
	}
	
	public void removeBlocks(List<TownBlock> b)
	{
		for(TownBlock block : b)
		{
			block.setTown(null);
			blocks.remove(block);
			MyTownDatasource.instance.unloadBlock(block);
		}
		save();
	}
	
	public void removeBlock(TownBlock block)
	{
		block.setTown(null);
		blocks.remove(block);
		MyTownDatasource.instance.unloadBlock(block);
		save();
	}
	
	public void removeResident(Resident res)
	{
		res.setTown(null);
		res.setRank(Rank.Resident);
		residents.remove(res);
		res.save();
	}
	
	public void deleteTown()
	{
		for(Resident res : residents)
			res.setTown(null);

		residents.clear();
		
		for(TownBlock block : blocks)
		{
			block.setTown(null);
			MyTownDatasource.instance.unloadBlock(block);
		}
		blocks.clear();
		
		settings.unlinkAllDown();
		MyTownDatasource.instance.deleteTown(this); // sets resident town to 0
		MyTownDatasource.instance.unloadTown(this);
	}
	
	public void save()
	{
		MyTownDatasource.instance.saveTown(this);
	}
	
	public void sendNotification(Level lvl, String msg)
	{
		String formatted = Formatter.townNotification(lvl, msg);
		for(Resident r : residents)
		{
			if (!r.isOnline())
				continue;

			r.onlinePlayer.sendChatToPlayer(formatted);
		}
	}
	
	public String serializeExtra()
	{
		return settings.serialize();
	}
	
	public void deserializeExtra(String val)
	{
		settings.deserialize(val);
	}
	
	public void sendTownInfo(ICommandSender pl, boolean adminInfo)
	{
		Town t = this;
		
		String extraBlocks = t.extraBlocks() == 0 ? "" :
			(t.extraBlocks() > 0 ? "+" : "-") + Math.abs(t.extraBlocks());
		
		StringBuilder mayors = new StringBuilder();
		StringBuilder assistants = new StringBuilder();
		StringBuilder residents = new StringBuilder();
		
		for(Resident r : t.residents())
		{
			if (r.rank() == Rank.Mayor)
			{
				if (mayors.length() > 0)
					mayors.append(", ");
				mayors.append(Formatter.formatResidentName(r));
			} 
			else if (r.rank() == Rank.Assistant)
			{
				if (assistants.length() > 0)
					assistants.append(", ");
				assistants.append(Formatter.formatResidentName(r));
			}
			else if (r.rank() == Rank.Resident)
			{
				if (residents.length() > 0)
					residents.append(", ");
				residents.append(Formatter.formatResidentName(r));
			}
		}
		
		if (mayors.length() < 1)
			mayors.append("none");
		if (assistants.length() < 1)
			assistants.append("none");
		if (residents.length() < 1)
			residents.append("none");
		
		StringBuilder blocks_list = new StringBuilder();
		
		if (adminInfo)
		{
			for (TownBlock block : blocks)
			{
				if (blocks_list.length() > 0)
					blocks_list.append(", ");
				blocks_list.append(String.format("(%s,%s)", block.x(), block.z()));
			}
		}
		
		String townColor = "§2";
		if (pl instanceof EntityPlayer)
		{
			Resident target = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)pl);
			if (target.town() != this)
				townColor = "§4"; 
		}
		
		pl.sendChatToPlayer(Term.TownStatusName.toString(townColor, t.name()));
		
		pl.sendChatToPlayer(Term.TownStatusGeneral.toString(t.blocks().size(), String.valueOf(t.allowedBlocksWOExtra()) + extraBlocks, !Town.bouncingOn ? "§6Disabled" : t.bounceNonMembers ? Term.TownBouncing.toString() : Term.TownNotBouncing.toString()));
		if (blocks_list.length() > 0)
			pl.sendChatToPlayer(blocks_list.toString());
		
		pl.sendChatToPlayer(Term.TownStatusMayor.toString(mayors.toString()));
		pl.sendChatToPlayer(Term.TownStatusAssistants.toString(assistants.toString()));
		pl.sendChatToPlayer(Term.TownStatusResidents.toString(residents.toString()));
	}
	
	public boolean canPluginChange(String plugin, Entity e)
	{
		return false;
	}
	
	public static boolean canPluginChangeWild(String plugin, String module, Entity e)
	{
		return true;
	}
	
	public boolean canPluginChange(String plugin, TileEntity e)
	{
		return false;
	}
	
	public static boolean canPluginChangeWild(String plugin, TileEntity e)
	{
		return true;
	}
	
	public void notifyPlayerLoggedOn(Resident r)
	{
		sendNotification(Level.INFO, Term.TownBroadcastLoggedIn.toString(r.name()));
	}
	
	public void notifyPlayerLoggedOff(Resident r)
	{
		sendNotification(Level.INFO, Term.TownBroadcastLoggedOut.toString(r.name()));
	}
	
	public boolean usesPlotOwnership()
	{
		for (TownBlock b : blocks)
		{
			if (b.owner() != null)
				return true;
		}
		
		return false;
	}
}
