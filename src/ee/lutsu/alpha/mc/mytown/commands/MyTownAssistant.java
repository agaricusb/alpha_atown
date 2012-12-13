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
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.event.PlayerEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;

public class MyTownAssistant 
{
	public static void handleCommand(ICommandSender cs, String[] args) throws CommandException
	{
		if (args.length < 1)
			return;
		
		if (!(cs instanceof EntityPlayer)) // no commands for console
			return;
		
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer)cs);
		if (res.town() == null || (res.rank() != Rank.Mayor && res.rank() != Rank.Assistant))
			return;
		
		String color = "6";
		if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()))
		{
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdClaim.toString(), Term.TownCmdClaimArgs.toString(), Term.TownCmdClaimDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdUnclaim.toString(), Term.TownCmdUnclaimArgs.toString(), Term.TownCmdUnclaimDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInvite.toString(), Term.TownCmdInviteArgs.toString(), Term.TownCmdInviteDesc.toString(), color));
			cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdKick.toString(), Term.TownCmdKickArgs.toString(), Term.TownCmdKickDesc.toString(), color));
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdClaim.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.claim")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (res.onlinePlayer == null)
				throw new NullPointerException("Onlineplayer is null");
			
			int radius_rec = 0;
			if (args.length > 1)
			{
				if (args[1].equalsIgnoreCase(Term.TownCmdClaimArgs1.toString()))
					radius_rec = Integer.parseInt(args[2]);
				else
					throw new CommandException(Term.TownErrCmdUnknownArgument, args[1]);
			}
			
			int cx = res.onlinePlayer.chunkCoordX;
			int cz = res.onlinePlayer.chunkCoordZ;
			int dim = res.onlinePlayer.dimension;
			
			StringBuilder sb = new StringBuilder();
			int nr = 0;
			
			try
			{
				for(int z = cz - radius_rec; z <= cz + radius_rec; z++)
				{
					for(int x = cx - radius_rec; x <= cx + radius_rec; x++)
					{
						TownBlock b = MyTownDatasource.instance.getOrMakeBlock(dim, x, z);
						if (b.town() == res.town())
							continue;
						
						try
						{
							res.town().addBlock(b);
							
							nr++;
							if (sb.length() > 0)
								sb.append(", ");
							sb.append(String.format("(%s,%s)", x, z));
						}
						catch(CommandException e)
						{
							MyTownDatasource.instance.unloadBlock(b);
							throw e;
						}
					}
				}
			}
			finally
			{
				// emulate that the player just entered it
				new PlayerEvents().enterChunk(new EnteringChunk(res.onlinePlayer, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ));
				cs.sendChatToPlayer(Term.TownBlocksClaimed.toString(nr, sb.toString()));
			}
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdUnclaim.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.unclaim")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (res.onlinePlayer == null)
				throw new NullPointerException("Onlineplayer is null");
			
			int radius_rec = 0;
			if (args.length > 1)
			{
				if (args[1].equalsIgnoreCase(Term.TownCmdUnclaimArgs1.toString()))
					radius_rec = Integer.parseInt(args[2]);
				else
					throw new CommandException(Term.TownErrCmdUnknownArgument, args[1]);
			}
			
			int cx = res.onlinePlayer.chunkCoordX;
			int cz = res.onlinePlayer.chunkCoordZ;
			int dim = res.onlinePlayer.dimension;
			
			StringBuilder sb = new StringBuilder();
			int nr = 0;
			ArrayList<TownBlock> blocks = new ArrayList<TownBlock>();

			for(int z = cz - radius_rec; z <= cz + radius_rec; z++)
			{
				for(int x = cx - radius_rec; x <= cx + radius_rec; x++)
				{
					TownBlock b = MyTownDatasource.instance.getBlock(dim, x, z);
					if (b == null || b.town() != res.town())
						continue;
					
					blocks.add(b);

					nr++;
					if (sb.length() > 0)
						sb.append(", ");
					sb.append(String.format("(%s,%s)", x, z));

				}
			}

			res.town().removeBlocks(blocks);
			
			// emulate that the player just entered it
			new PlayerEvents().enterChunk(new EnteringChunk(res.onlinePlayer, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ));
			cs.sendChatToPlayer(Term.TownBlocksUnclaimed.toString(nr, sb.toString()));
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdInvite.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.invite")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length < 2)
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdInvite.toString(), Term.TownCmdInviteArgs.toString(), Term.TownCmdInviteDesc.toString(), color));
			else
			{
				Resident target = MyTownDatasource.instance.getResident(args[1]);
				if (target == null || target.onlinePlayer == null)
					throw new CommandException(Term.TownErrPlayerNotFoundOrOnline);
				
				if (target == res)
					throw new CommandException(Term.TownErrInvitationSelf);
				if (target.town() == res.town())
					throw new CommandException(Term.TownErrInvitationAlreadyInYourTown);
				if (target.town() != null)
					throw new CommandException(Term.TownErrInvitationInTown);
				if (target.inviteActiveFrom != null)
					throw new CommandException(Term.TownErrInvitationActive);
				
				target.inviteActiveFrom = res.town();
				
				target.onlinePlayer.sendChatToPlayer(Term.TownInvitation.toString(res.name(), res.town().name()));
				cs.sendChatToPlayer(Term.TownInvitedPlayer.toString(target.name()));
			}
		}
		else if (args[0].equalsIgnoreCase(Term.TownCmdKick.toString()))
		{
			if (!Permissions.canAccess(res, "mytown.cmd.kick")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
			
			if (args.length < 2)
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdKick.toString(), Term.TownCmdKickArgs.toString(), Term.TownCmdKickDesc.toString(), color));
			else
			{
				Resident target = MyTownDatasource.instance.getResident(args[1]);
				
				if (target == null) // all town residents are always loaded
					throw new CommandException(Term.TownErrPlayerNotFound);
				
				if (target == res)
					throw new CommandException(Term.TownErrCannotKickYourself);
				if (target.town() != res.town())
					throw new CommandException(Term.TownErrPlayerNotInYourTown);
				if (target.rank() == Rank.Mayor && res.rank() == Rank.Assistant)
					throw new CommandException(Term.TownErrCannotKickMayor);
				if (target.rank() == Rank.Assistant && res.rank() == Rank.Assistant)
					throw new CommandException(Term.TownErrCannotKickAssistants);
				
				res.town().removeResident(target);
				
				res.town().sendNotification(Level.INFO, Term.TownKickedPlayer.toString(res.name(), target.name()));
			}
		}
	}
}
