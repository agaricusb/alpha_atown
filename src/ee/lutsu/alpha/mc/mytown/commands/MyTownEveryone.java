package ee.lutsu.alpha.mc.mytown.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.Town;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;

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
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdMap.toString()))
			{
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
					
					t.sendTownInfo(cs, res.isOp());
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), color));
			}
		}
		else
		{
			if (args.length < 1 || (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString())))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInfo.toString(), Term.TownCmdInfoArgs.toString(), Term.TownCmdInfoDesc.toString(), null));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdList.toString(), "", Term.TownCmdListDesc.toString(), null));
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase(Term.TownCmdInfo.toString()))
			{
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
			TreeMap<String, Town> sorted = new TreeMap<String, Town>();
			for (Town t : MyTownDatasource.instance.towns)
				sorted.put(t.name(), t);
			
			StringBuilder sb = new StringBuilder();
			sb.append(Term.TownCmdListStart.toString(""));
			int i = 0;
			
			for(Map.Entry<String, Town> e : sorted.entrySet())
			{
				String n = Term.TownCmdListEntry.toString(e.getValue().name(), e.getValue().residents().size());
				if (i > 0)
				{
					sb.append(", ");
					
					if (sb.length() + n.length() >= 100)
					{
						cs.sendChatToPlayer(sb.toString());
						sb.setLength(0);
					}
				}
				i++;
				sb.append(n);
			}
			
			if (sb.length() > 0)
				cs.sendChatToPlayer(sb.toString());
		}
	}
}
