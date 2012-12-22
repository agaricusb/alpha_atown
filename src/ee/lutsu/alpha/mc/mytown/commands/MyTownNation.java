package ee.lutsu.alpha.mc.mytown.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Nation;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.Town;

public class MyTownNation 
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
		
		Town town = res.town();
		Nation nation = town.nation();
		String color = "2";
		if (nation == null) // not in nation - new, accept, reject
		{
			if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()) || (args[0].equalsIgnoreCase(Term.TownCmdNation.toString()) && args.length < 2))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationNew.toString(), Term.TownCmdNationNewArgs.toString(), Term.TownCmdNationNewDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationAccept.toString(), null, Term.TownCmdNationAcceptDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationReject.toString(), null, Term.TownCmdNationRejectDesc.toString(), color));
			}
			else if (!args[0].equalsIgnoreCase(Term.TownCmdNation.toString()))
				return;
			else if (args[1].equalsIgnoreCase(Term.TownCmdNationNew.toString()))
			{
				if (args.length == 3)
				{
					String name = args[2];
					Nation n = new Nation(name, town);
					
					String msg = Term.NationBroadcastCreated.toString(town.name(), n.name());
					for(Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
					{
						((EntityPlayer)obj).sendChatToPlayer(msg);
					}
					
					town.sendTownInfo(cs, res.isOp());
				}
				else
					cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationNew.toString(), Term.TownCmdNationNewArgs.toString(), Term.TownCmdNationNewDesc.toString(), color));
			}
		}
		else if (nation.capital() == res.town()) // capitol city - invite, delete, kick, transfer
		{
			if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()) || (args[0].equalsIgnoreCase(Term.TownCmdNation.toString()) && args.length < 2))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationInvite.toString(), Term.TownCmdNationInviteArgs.toString(), Term.TownCmdNationInviteDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationKick.toString(), Term.TownCmdNationKickArgs.toString(), Term.TownCmdNationKickDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationTransfer.toString(), Term.TownCmdNationTransferArgs.toString(), Term.TownCmdNationTransferDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationDel.toString(), null, Term.TownCmdNationDelDesc.toString(), color));
			}
			else if (!args[0].equalsIgnoreCase(Term.TownCmdNation.toString()))
				return;
			else if (args[1].equalsIgnoreCase(Term.TownCmdNationInvite.toString()))
			{
				
			}
		}
		else // member town - leave
		{
			if (args[0].equals("?") || args[0].equalsIgnoreCase(Term.CommandHelp.toString()) || (args[0].equalsIgnoreCase(Term.TownCmdNation.toString()) && args.length < 2))
			{
				cs.sendChatToPlayer(Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationLeave.toString(), null, Term.TownCmdNationLeaveDesc.toString(), color));
			}
			else if (!args[0].equalsIgnoreCase(Term.TownCmdNation.toString()))
				return;
			else if (args[1].equalsIgnoreCase(Term.TownCmdNationLeave.toString()))
			{
				
			}
		}
	}
}
