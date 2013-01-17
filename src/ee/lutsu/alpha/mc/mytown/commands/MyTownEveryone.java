package ee.lutsu.alpha.mc.mytown.commands;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.entities.Resident.Rank;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class MyTownEveryone 
{
	public static List<String> getAutoComplete(ICommandSender cs, String[] args)
	{
		ArrayList<String> list = new ArrayList<String>();

		if (args.length == 1)
		{
			list.add(Term.CommandHelp.toString());
			list.add(Term.TownCmdInfo.toString());
			list.add(Term.TownCmdList.toString());
			list.add(Term.TownCmdRes.toString());
			
			if (cs instanceof EntityPlayer)
			{
				list.add(Term.TownCmdMap.toString());
				list.add(Term.TownCmdFriend.toString());
				list.add(Term.TownCmdSpawn.toString());
			}
		}
		else if (args.length == 2 && (args[0].equalsIgnoreCase(Term.TownCmdInfo.toString()) || args[0].equalsIgnoreCase(Term.TownCmdSpawn.toString())))
		{
			for (Town t : MyTownDatasource.instance.towns)
				list.add(t.name());
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase(Term.TownCmdRes.toString()))
		{
			for (Resident r : MyTownDatasource.instance.residents)
				list.add(r.name());
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase(Term.TownCmdMap.toString()))
		{
			list.add("on");
			list.add("off");
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase(Term.TownCmdFriend.toString()))
		{
			list.add(Term.TownCmdFriendArgsAdd.toString());
			list.add(Term.TownCmdFriendArgsRemove.toString());
		}
		else if (args.length == 3 && args[0].equalsIgnoreCase(Term.TownCmdFriend.toString()))
		{
			Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
			String cmd = args[1];
			
			for (Resident r : MyTownDatasource.instance.residents)
			{
				if (cmd.equalsIgnoreCase(Term.TownCmdFriendArgsAdd.toString()) && res.friends.contains(r))
					continue;
				
				if (cmd.equalsIgnoreCase(Term.TownCmdFriendArgsRemove.toString()) && !res.friends.contains(r))
					continue;
				
				list.add(r.name());
			}
		}
		
		return list;
	}
	
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if ((((!(cs instanceof EntityPlayer) || MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs).town() == null) && args.length == 0) || (args.length > 0 && (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString())))))
		{	
			cs.sendChatToPlayer(Term.LineSeperator.toString());
			
			if (args.length > 1)
				cs.sendChatToPlayer(Term.CommandHelpStartSub.toString(args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase()));
			else
				cs.sendChatToPlayer(Term.CommandHelpStart.toString());
		}

		
		if (cs instanceof EntityPlayer)
		{
			Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
	
			String color = "f";
			if ((res.town() == null && args.length == 0) || (args.length == 1 && (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdMap.toString(), Term.TownCmdMapArgs.toString(), Term.TownCmdMapDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdList.toString(), "", Term.TownCmdListDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRes.toString(), Term.TownCmdResArgs.toString(), Term.TownCmdResDesc.toString(), null));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdFriend.toString(), Term.TownCmdFriendArgs.toString(), Term.TownCmdFriendDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdSpawn.toString(), Term.TownCmdSpawnArgs.toString(), Term.TownCmdSpawnDesc.toString(), color));
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdMap.toString()))
			{
				if (!Permissions.canAccess(res, "mytown.cmd.map")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (args.length > 1)
				{
					boolean modeOn = !res.mapMode;
					
					if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("activate"))
						modeOn = true;
					else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("deactivate"))
						modeOn = false;
					
					res.mapMode = modeOn;
					
					String msg = res.mapMode ? Term.PlayerMapModeOn.toString() : Term.PlayerMapModeOff.toString();
					cs.sendChatToPlayer(msg);
				}
				else
					res.sendLocationMap(res.onlinePlayer.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ);
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdInfo.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.info")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (args.length == 2)
				{
					Town t = MyTownDatasource.instance.getTown(args[1]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, args[1]);
					
					t.sendTownInfo(cs, res.shouldShowTownBlocks());
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), color));
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdFriend.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.friend")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (args.length == 3)
				{
					String cmd = args[1];
					Resident target = MyTownDatasource.instance.getResident(args[2]);
					if (target == null)
						throw new CommandException(Term.TownErrPlayerNotFound);
					
					if (cmd.equalsIgnoreCase(Term.TownCmdFriendArgsAdd.toString()))
					{
						if (!res.addFriend(target))
							throw new CommandException(Term.ErrPlayerAlreadyInFriendList, res.name());
					}
					else if (cmd.equalsIgnoreCase(Term.TownCmdFriendArgsRemove.toString()))
					{
						if (!res.removeFriend(target))
							throw new CommandException(Term.ErrPlayerNotInFriendList, res.name());
					}
					res.sendInfoTo(cs, res.shouldShowPlayerLocation());
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdFriend.toString(), Term.TownCmdFriendArgs.toString(), Term.TownCmdFriendDesc.toString(), color));
			}
			else if (args.length > 0 && args[0].equals(Term.TownCmdSpawn.toString()))
			{
				Town target = null;
				if (args.length < 2)
				{
					if (!Permissions.canAccess(cs, "mytown.cmd.spawn.own")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
					
					if (res.town() == null)
						throw new CommandException(Term.ErrPermYouDontHaveTown);
					
					target = res.town();
				}
				else
				{
					if (!Permissions.canAccess(cs, "mytown.cmd.spawn.other")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
					
					Town t = MyTownDatasource.instance.getTown(args[1]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, args[1]);
					
					target = t;
				}
				if (target.spawnBlock == null || target.getSpawn() == null)
					throw new CommandException(Term.TownErrSpawnNotSet);
				
				res.sendToTownSpawn(target);
			}
		}
		else
		{
			if (args.length < 1 || (args.length == 1 && (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), null));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdList.toString(), "", Term.TownCmdListDesc.toString(), null));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRes.toString(), Term.TownCmdResArgs.toString(), Term.TownCmdResDesc.toString(), null));
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdInfo.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.info")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (args.length == 2)
				{
					Town t = MyTownDatasource.instance.getTown(args[1]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, args[1]);
					
					t.sendTownInfo(cs, true);
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), null));
			}
		}
		
		if (args.length > 0 && args[0].equals(Term.TownCmdList.toString()))
		{
			if (!Permissions.canAccess(cs, "mytown.cmd.list")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			ArrayList<Town> sorted = new ArrayList<Town>(MyTownDatasource.instance.towns);
			
			Collections.sort(sorted, new Comparator<Town>()
			{
				@Override
				public int compare(Town arg0, Town arg1)
				{
					return Integer.compare(arg1.residents().size(), arg0.residents().size());
				}
			});

			StringBuilder sb = new StringBuilder();
			sb.append(Term.TownCmdListStart.toString(sorted.size(), ""));
			int i = 0;
			
			for (Town e : sorted)
			{
				String n = Term.TownCmdListEntry.toString(e.name(), e.residents().size());
				if (i > 0)
				{
					sb.append(", ");
					/*
					if (sb.length() + n.length() > 70)
					{
						cs.sendChatToPlayer(sb.toString());
						sb = new StringBuilder();
						i = 0;
					}*/
				}
				i++;
				sb.append(n);
			}
			
			if (sb.length() > 0)
				cs.sendChatToPlayer(sb.toString());
		}
		else if (args.length > 0 && args[0].equals(Term.TownCmdRes.toString()))
		{
			if (!Permissions.canAccess(cs, "mytown.cmd.res")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length == 1 && cs instanceof EntityPlayer)
			{
				Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
				res.sendInfoTo(cs, res.shouldShowPlayerLocation());
			}
			else if (args.length == 2)
			{
				Resident r = MyTownDatasource.instance.getResident(args[1]);
				if (r == null)
					cs.sendChatToPlayer(Term.TownErrPlayerNotFound.toString());
				else
				{
					r.sendInfoTo(cs, cs instanceof EntityPlayer ? MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs).shouldShowPlayerLocation() : true);
				}
			}
			else
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRes.toString(), Term.TownCmdResArgs.toString(), Term.TownCmdResDesc.toString(), null));
			}
		}
	}
}
