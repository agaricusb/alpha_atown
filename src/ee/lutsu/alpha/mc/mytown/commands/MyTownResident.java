package ee.lutsu.alpha.mc.mytown.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.Town;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;

public class MyTownResident 
{
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if (!(cs instanceof EntityPlayer)) // no commands for console
			return;
		
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
		if (res.town() == null)
			return;
		
		String color = "f";
		if (args.length < 1)
		{
			if (!Permissions.canAccess(res, "mytown.cmd.info")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			res.town().sendTownInfo(res.onlinePlayer, res.isOp());
		}
		else
		{
			if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdLeave.toString(), "", Term.TownCmdLeaveDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdOnline.toString(), "", Term.TownCmdOnlineDesc.toString(), color));
			}
			else if (args[0].equalsIgnoreCase(Term.TownCmdLeave.toString()))
			{
				if (!Permissions.canAccess(res, "mytown.cmd.leave")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (res.rank() == Rank.Mayor)
					throw new CommandException(Term.TownErrMayorsCantLeaveTheTown);
				
				Town t = res.town();
				t.sendNotification(Level.INFO, Term.TownPlayerLeft.toString(res.name()));
				t.removeResident(res);
			}
			else if (args[0].equalsIgnoreCase(Term.TownCmdOnline.toString()))
			{
				if (!Permissions.canAccess(res, "mytown.cmd.online")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				Town t = res.town();
				
				StringBuilder sb = new StringBuilder();
				for(Resident r : t.residents())
				{
					if (!r.isOnline())
						continue;
					
					if (sb.length() > 0)
						sb.append(", ");
					
					sb.append(Formatter.formatResidentName(r));
				}
				
				cs.sendChatToPlayer(Term.TownPlayersOnlineStart.toString(sb.toString()));
			}
			else if (args[0].equalsIgnoreCase(Term.TownCmdPerm.toString()))
			{
				if (args.length < 2)
				{
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownCmdPerm.toString(), Term.TownCmdPermArgs.toString(), Term.TownCmdPermDesc.toString(), color));
					return;
				}
				
				String node = args[1];
				if (!node.equalsIgnoreCase(Term.TownCmdPermArgsTown.toString()) && !node.equalsIgnoreCase(Term.TownCmdPermArgsResident.toString()) && !node.equalsIgnoreCase(Term.TownCmdPermArgsPlot.toString()))
				{
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownCmdPerm.toString(), Term.TownCmdPermArgs.toString(), Term.TownCmdPermDesc.toString(), color));
					return;
				}
				
				if (args.length < 3) // show
				{
					if (!Permissions.canAccess(cs, "mytown.cmd.perm.show." + node)) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
					showPermissions(cs, res, node);
				}
				else
				{
					String action = args[2];
					if (action.equalsIgnoreCase(Term.TownCmdPermArgs2Set.toString()) && args.length > 3)
					{
						if (!Permissions.canAccess(cs, "mytown.cmd.perm.set." + node + "." + args[3])) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
						setPermissions(cs, res, node, args[3], args.length > 4 ? args[4] : null);
					}
					else if (action.equalsIgnoreCase(Term.TownCmdPermArgs2Force.toString()))
					{
						if (!Permissions.canAccess(cs, "mytown.cmd.perm.force." + node)) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
						
						flushPermissions(cs, res, node);
					}
					else
						cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownCmdPerm.toString(), Term.TownCmdPermArgs.toString(), Term.TownCmdPermDesc.toString(), color));
				}
			}
		}
	}
	
	private static TownSettingCollection getPermNode(String node, Resident res) throws CommandException
	{
		TownSettingCollection set = null;
		if (node.equalsIgnoreCase(Term.TownCmdPermArgsTown.toString()))
		{
			if (res.town() == null)
				throw new CommandException(Term.ErrPermYouDontHaveTown);
			
			set = res.town().settings;
		}
		else if (node.equalsIgnoreCase(Term.TownCmdPermArgsResident.toString()))
			set = res.settings;
		else if (node.equalsIgnoreCase(Term.TownCmdPermArgsPlot.toString()))
		{
			TownBlock block = MyTownDatasource.instance.getBlock(res.onlinePlayer.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ);
			if (block == null || block.town() == null)
				throw new CommandException(Term.ErrPermPlotNotInTown);
			
			if (block.town() != res.town())
				throw new CommandException(Term.ErrPermPlotNotInYourTown);
			
			set = block.settings;
		}
		else
			throw new CommandException(Term.ErrPermSettingCollectionNotFound, node);
		
		return set;
	}

	private static void showPermissions(ICommandSender sender, Resident res, String node) throws CommandException
	{
		TownSettingCollection set = getPermNode(node, res);
		
		String title = "";
		if (node.equalsIgnoreCase(Term.TownCmdPermArgsTown.toString()))
			title = "your town '" + res.town().name() + "' (default for residents)";
		else if (node.equalsIgnoreCase(Term.TownCmdPermArgsResident.toString()))
			title = "you '" + res.name() + "'";
		else if (node.equalsIgnoreCase(Term.TownCmdPermArgsPlot.toString()))
		{
			TownBlock block = (TownBlock)set.tag;
			title = String.format("the plot @ dim %s, %s,%s owned by '%s'", block.worldDimension(), block.x(), block.z(), block.ownerDisplay());
		}
		
		set.show(sender, title);
	}
	
	private static void flushPermissions(ICommandSender sender, Resident res, String node) throws CommandException
	{
		TownSettingCollection set = getPermNode(node, res);
		
		if (node.equalsIgnoreCase(Term.TownCmdPermArgsTown.toString()) || node.equalsIgnoreCase(Term.TownCmdPermArgsPlot.toString()))
		{
			if (res.rank() == Rank.Resident)
				throw new CommandException(Term.ErrPermRankNotEnough);
		}
		
		if (set.childs.size() < 1)
			throw new CommandException(Term.ErrPermNoChilds);
		
		set.forceChildsToInherit();
		sender.sendChatToPlayer(Term.PermForced.toString(node));
	}
	
	private static void setPermissions(ICommandSender sender, Resident res, String node, String key, String val) throws CommandException
	{
		TownSettingCollection set = getPermNode(node, res);

		if (node.equalsIgnoreCase(Term.TownCmdPermArgsTown.toString()) || node.equalsIgnoreCase(Term.TownCmdPermArgsPlot.toString()))
		{
			if (res.rank() == Rank.Resident)
				throw new CommandException(Term.ErrPermRankNotEnough);
		}
		
		set.setValue(key, val);
		
		showPermissions(sender, res, node);
		sender.sendChatToPlayer(Term.PermSetDone.toString(key, node));
	}
}
