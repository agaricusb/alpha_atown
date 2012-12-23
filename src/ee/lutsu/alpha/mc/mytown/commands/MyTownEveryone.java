package ee.lutsu.alpha.mc.mytown.commands;

import java.util.AbstractMap;
import java.util.ArrayList;
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

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class MyTownEveryone 
{
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if (cs instanceof EntityPlayer)
		{
			Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
	
			String color = "f";
			if ((res.town() == null && args.length == 0) || (args.length > 0 && (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))))
			{
				cs.sendChatToPlayer(Term.LineSeperator.toString());

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
					res.sendInfoTo(cs);
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
			if (args.length < 1 || (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString())))
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
			
			TreeSet<Town> sorted = new TreeSet<Town>(new Comparator<Town>()
			{
				@Override
				public int compare(Town arg0, Town arg1)
				{
					return Integer.compare(arg1.residents().size(), arg0.residents().size());
				}
			});
			
			sorted.addAll(MyTownDatasource.instance.towns);
			
			StringBuilder sb = new StringBuilder();
			sb.append(Term.TownCmdListStart.toString(""));
			int i = 0;
			
			for (Town e : sorted)
			{
				String n = Term.TownCmdListEntry.toString(e.name(), e.residents().size());
				if (i > 0)
				{
					sb.append(", ");
					
					if (sb.length() + n.length() >= 100)
					{
						cs.sendChatToPlayer(sb.toString());
						sb.setLength(0);
						i = 0;
					}
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
				res.sendInfoTo(cs);
			}
			else if (args.length == 2)
			{
				Resident r = MyTownDatasource.instance.getResident(args[1]);
				if (r == null)
					cs.sendChatToPlayer(Term.TownErrPlayerNotFound.toString());
				else
				{
					r.sendInfoTo(cs);
				}
			}
			else
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRes.toString(), Term.TownCmdResArgs.toString(), Term.TownCmdResDesc.toString(), null));
			}
		}
	}
}
