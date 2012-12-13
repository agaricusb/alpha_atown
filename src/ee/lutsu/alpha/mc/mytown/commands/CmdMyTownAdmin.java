package ee.lutsu.alpha.mc.mytown.commands;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityAnimal;
import net.minecraft.src.EntityArrow;
import net.minecraft.src.EntityBat;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.EntitySlime;
import net.minecraft.src.EntitySquid;
import net.minecraft.src.ICommandSender;

public class CmdMyTownAdmin extends CommandBase
{
	@Override
	public String getCommandName() 
	{
		return Term.TownAdmCommand.toString();
	}
	
	@Override
    public List getCommandAliases()
    {
		return Arrays.asList(Term.TownAdmCommandAliases.toString().split(" "));
    }
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender cs)
	{
		return Permissions.canAccess(cs, "mytown.cmd.adm");
	}
	
	@Override
    public String getCommandUsage(ICommandSender par1ICommandSender)
    {
		return "/" + getCommandName();
    }

	@Override
	public void processCommand(ICommandSender cs, String[] var2) 
	{
		try
		{
			MyTownDatasource src = MyTownDatasource.instance;
			String color = "9";
			if (var2.length == 0 || var2[0].equals("?") || var2[0].equalsIgnoreCase(Term.CommandHelp.toString()))
			{
				cs.sendChatToPlayer(Term.LineSeperator.toString());

				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdNew.toString(), Term.TownadmCmdNewArgs.toString(), Term.TownadmCmdNewDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdDelete.toString(), Term.TownadmCmdDeleteArgs.toString(), Term.TownadmCmdDeleteDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdSet.toString(), Term.TownadmCmdSetArgs.toString(), Term.TownadmCmdSetDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdRem.toString(), Term.TownadmCmdRemArgs.toString(), Term.TownadmCmdRemDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdExtra.toString(), Term.TownadmCmdExtraArgs.toString(), Term.TownadmCmdExtraDesc.toString(), color));
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdReload.toString(), "", Term.TownadmCmdReloadDesc.toString(), color));
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdReload.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.reload")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				MyTown.instance.reload();
				cs.sendChatToPlayer(Term.TownadmModReloaded.toString());
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdNew.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.new")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (var2.length != 3)
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdNew.toString(), Term.TownadmCmdNewArgs.toString(), Term.TownadmCmdNewDesc.toString(), color));
				else
				{
					Resident r = src.getOrMakeResident(var2[2]);
					Town t = new Town(var2[1], r, null);
					cs.sendChatToPlayer(Term.TownadmCreatedNewTown.toString(t.name(), r.name()));
				}
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdDelete.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.delete")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (var2.length != 2)
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdDelete.toString(), Term.TownadmCmdDeleteArgs.toString(), Term.TownadmCmdDeleteDesc.toString(), color));
				else
				{
					Town t = src.getTown(var2[1]);
					
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, var2[1]);
					
					t.deleteTown();
					cs.sendChatToPlayer(Term.TownadmDeletedTown.toString(t.name()));
				}
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdSet.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.set")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (var2.length < 4)
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdSet.toString(), Term.TownadmCmdSetArgs.toString(), Term.TownadmCmdSetDesc.toString(), color));
				else
				{
					Town t = src.getTown(var2[1]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, var2[1]);
					
					Rank rank = Rank.parse(var2[2]);
					
					for(int i = 3; i < var2.length; i++)
					{
						Resident r = src.getOrMakeResident(var2[i]);
						if (r.town() != null)
						{
							if (r.town() != t)
							{
								r.town().removeResident(r); // unloads the resident
								r = src.getOrMakeResident(var2[i]);
							}
						}
						else
							t.addResident(r);
						
						t.setResidentRank(r, rank);
					}
					cs.sendChatToPlayer(Term.TownadmResidentsSet.toString());
				}
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdRem.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.rem")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (var2.length < 3)
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdRem.toString(), Term.TownadmCmdRemArgs.toString(), Term.TownadmCmdRemDesc.toString(), color));
				else
				{
					Town t = src.getTown(var2[1]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, var2[1]);

					for(int i = 2; i < var2.length; i++)
					{
						Resident r = src.getOrMakeResident(var2[i]);
						if (r.town() != null && r.town() == t)
						{
							t.removeResident(r); // unloads the resident
						}
					}
					cs.sendChatToPlayer(Term.TownadmResidentsSet.toString());
				}
			}
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdExtra.toString()))
			{
				if (!Permissions.canAccess(cs, "mytown.cmd.adm.extra")) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
				
				if (var2.length < 3)
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdExtra.toString(), Term.TownadmCmdExtraArgs.toString(), Term.TownadmCmdExtraDesc.toString(), color));
				else
				{
					Town t = src.getTown(var2[1]);
					int cnt = Integer.parseInt(var2[2]);
					if (t == null)
						throw new CommandException(Term.TownErrNotFound, var2[1]);

					t.setExtraBlocks(cnt);
					cs.sendChatToPlayer(Term.TownadmExtraSet.toString());
				}
			}
		}
		catch(CommandException ex)
		{
			cs.sendChatToPlayer(Formatter.commandError(Level.WARNING, ex.errorCode.toString(ex.args)));
		}
		catch(Exception ex)
		{
			cs.sendChatToPlayer(Formatter.commandError(Level.SEVERE, ex.toString()));
		}
	}

}
