package ee.lutsu.alpha.mc.mytown.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
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
		}
	}
}
