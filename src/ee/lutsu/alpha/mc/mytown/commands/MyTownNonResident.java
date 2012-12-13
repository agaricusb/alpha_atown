package ee.lutsu.alpha.mc.mytown.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.event.PlayerEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;

public class MyTownNonResident 
{
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if (!(cs instanceof EntityPlayer)) // no commands for console
			return;
		
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
		if (res.town() != null)
			return;
		
		String color = "f";
		if (args.length < 1 || args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))
		{
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNew.toString(), Term.TownCmdNewArgs.toString(), Term.TownCmdNewDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdAccept.toString(), "", Term.TownCmdAcceptDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdDeny.toString(), "", Term.TownCmdDenyDesc.toString(), color));
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdNew.toString()))
		{
			if (args.length < 2 || args.length > 2)
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNew.toString(), Term.TownCmdNewArgs.toString(), Term.TownCmdNewDesc.toString(), color));
			else
			{
				Town t = new Town(args[1], res, MyTownDatasource.instance.getOrMakeBlock(res.onlinePlayer.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ));
				
				// emulate that the player just entered it
				new PlayerEvents().enterChunk(new EnteringChunk(res.onlinePlayer, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ));
				
				String msg = Term.TownBroadcastCreated.toString(res.name(), t.name());
				for(Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
				{
					((EntityPlayer)obj).sendChatToPlayer(msg);
				}
				
				t.sendTownInfo(cs, res.isOp());
			}
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdAccept.toString()))
		{
			if (res.inviteActiveFrom == null)
				throw new CommandException(Term.TownErrYouDontHavePendingInvitations);
			
			res.setRank(Rank.Resident);
			res.inviteActiveFrom.addResident(res);
			
			res.inviteActiveFrom.sendNotification(Level.INFO, Term.TownPlayerJoinedTown.toString(res.name()));
			res.inviteActiveFrom = null;
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdDeny.toString()))
		{
			if (res.inviteActiveFrom == null)
				throw new CommandException(Term.TownErrYouDontHavePendingInvitations);
			
			res.inviteActiveFrom = null;

			res.onlinePlayer.sendChatToPlayer(Term.TownPlayerDeniedInvitation.toString());
		}
	}
}
