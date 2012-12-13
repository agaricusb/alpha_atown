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
import ee.lutsu.alpha.mc.mytown.event.PlayerEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;

public class MyTownMayor 
{
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if (args.length < 1)
			return;
		
		if (!(cs instanceof EntityPlayer)) // no commands for console
			return;
		
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
		if (res.town() == null || res.rank() != Rank.Mayor)
			return;
		
		String color = "c";
		if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))
		{
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdAssistant.toString(), Term.TownCmdAssistantArgs.toString(), Term.TownCmdAssistantDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdMayor.toString(), Term.TownCmdMayorArgs.toString(), Term.TownCmdMayorDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdBounce.toString(), "", Term.TownCmdBounceDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRename.toString(), Term.TownCmdRenameArgs.toString(), Term.TownCmdRenameDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdDelete.toString(), "", Term.TownCmdDeleteDesc.toString(), color));
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdAssistant.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.assistant")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length != 3)
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdAssistant.toString(), Term.TownCmdAssistantArgs.toString(), Term.TownCmdAssistantDesc.toString(), color));
			else
			{
				String cmd = args[1];
				String name = args[2];
				
				Resident r = MyTownDatasource.instance.getResident(name);
				if (r == null)
					throw new CommandException(Term.TownErrPlayerNotFound);
				if (r == res)
					throw new CommandException(Term.TownErrCannotDoWithYourself);
				if (r.town() != res.town())
					throw new CommandException(Term.TownErrPlayerNotInYourTown);

				if (cmd.equalsIgnoreCase(Term.TownCmdAssistantArgs1.toString())) // add
				{
					if (r.rank() == Rank.Mayor)
						throw new CommandException(Term.TownErrCannotUseThisDemoteMayor);
					if (r.rank() == Rank.Assistant)
						throw new CommandException(Term.TownErrPlayerIsAlreadyAssistant);
					
					res.town().setResidentRank(r, Rank.Assistant);
					res.town().sendNotification(Level.INFO, Term.TownPlayerPromotedToAssistant.toString(r.name()));
				}
				else if (cmd.equalsIgnoreCase(Term.TownCmdAssistantArgs2.toString())) // remove
				{
					if (r.rank() != Rank.Assistant)
						throw new CommandException(Term.TownErrPlayerIsNotAssistant);
					
					res.town().setResidentRank(r, Rank.Resident);
					res.town().sendNotification(Level.INFO, Term.TownPlayerDemotedFromAssistant.toString(r.name()));
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdAssistant.toString(), Term.TownCmdAssistantArgs.toString(), Term.TownCmdAssistantDesc.toString(), color));
			}
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdMayor.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.mayor")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length != 2)
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdMayor.toString(), Term.TownCmdMayorArgs.toString(), Term.TownCmdMayorDesc.toString(), color));
			else
			{
				String name = args[1];
				
				Resident r = MyTownDatasource.instance.getResident(name);
				if (r == null)
					throw new CommandException(Term.TownErrPlayerNotFound);
				if (r == res)
					throw new CommandException(Term.TownErrCannotDoWithYourself);
				if (r.town() != res.town())
					throw new CommandException(Term.TownErrPlayerNotInYourTown);

				res.town().setResidentRank(r, Rank.Mayor);
				res.town().setResidentRank(res, Rank.Assistant);
				 
				res.town().sendNotification(Level.INFO, Term.TownPlayerPromotedToMayor.toString(r.name()));
			}
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdDelete.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.delete")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length == 2 && args[1].equalsIgnoreCase("ok"))
			{
				String name = res.town().name();
				res.town().deleteTown();
				
				// emulate that the player just entered it
				new PlayerEvents().enterChunk(new EnteringChunk(res.onlinePlayer, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ));
				
				String msg = Term.TownBroadcastDeleted.toString(name);
				for(Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
				{
					((EntityPlayer)obj).sendChatToPlayer(msg);
				}
			}
			else
				cs.sendChatToPlayer(Term.TownCmdDeleteAction.toString());
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdBounce.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.bounce")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			res.town().setBounce(!res.town().bounceNonMembers);
			
			cs.sendChatToPlayer(Term.TownBouncingChanged.toString(res.town().bounceNonMembers ? Term.TownBouncing.toString() : Term.TownNotBouncing.toString()));
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdRename.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.rename")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length == 2)
			{
				res.town().setTownName(args[1]);
				res.town().sendNotification(Level.INFO, Term.TownRenamed.toString(res.town().name()));
			}
			else
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdRename.toString(), Term.TownCmdRenameArgs.toString(), Term.TownCmdRenameDesc.toString(), color));
		}
	}
}
