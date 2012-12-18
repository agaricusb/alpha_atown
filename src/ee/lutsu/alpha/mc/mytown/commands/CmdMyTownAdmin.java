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
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;
import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection;
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
		return Permissions.canAccess(cs, "mytown.adm.cmd");
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
				cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdPerm.toString(), Term.TownadmCmdPermArgs.toString(), Term.TownadmCmdPermDesc.toString(), color));
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
			else if (var2[0].equalsIgnoreCase(Term.TownadmCmdPerm.toString()))
			{
				if (var2.length < 2)
				{
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdPerm.toString(), Term.TownadmCmdPermArgs.toString(), Term.TownadmCmdPermDesc.toString(), color));
					return;
				}
				
				String node = var2[1];
				if (!node.equalsIgnoreCase(Term.TownadmCmdPermArgsServer.toString()) && !node.equalsIgnoreCase(Term.TownadmCmdPermArgsWild.toString()) && !node.toLowerCase().startsWith(Term.TownadmCmdPermArgsWild2.toString().toLowerCase()))
				{
					cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdPerm.toString(), Term.TownadmCmdPermArgs.toString(), Term.TownadmCmdPermDesc.toString(), color));
					return;
				}
				
				if (var2.length < 3) // show
				{
					if (!Permissions.canAccess(cs, "mytown.cmd.adm.perm.show." + node)) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
					showPermissions(cs, node);
				}
				else
				{
					String action = var2[2];
					if (action.equalsIgnoreCase(Term.TownadmCmdPermArgs2Set.toString()) && var2.length > 3)
					{
						if (!Permissions.canAccess(cs, "mytown.cmd.adm.perm.set." + node + "." + var2[3])) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
						setPermissions(cs, node, var2[3], var2.length > 4 ? var2[4] : null);
					}
					else if (action.equalsIgnoreCase(Term.TownadmCmdPermArgs2Force.toString()))
					{
						if (!Permissions.canAccess(cs, "mytown.cmd.adm.perm.force." + node)) { cs.sendChatToPlayer(Term.ErrCannotAccessCommand.toString()); return; }
						
						flushPermissions(cs, node);
					}
					else
						cs.sendChatToPlayer(Formatter.formatAdminCommand(Term.TownadmCmdPerm.toString(), Term.TownadmCmdPermArgs.toString(), Term.TownadmCmdPermDesc.toString(), color));
				}
			}
		}
		catch(CommandException ex)
		{
			cs.sendChatToPlayer(Formatter.commandError(Level.WARNING, ex.errorCode.toString(ex.args)));
		}
		catch(Throwable ex)
		{
			Log.log(Level.WARNING, String.format("Admin command execution error by %s", cs), ex);
			cs.sendChatToPlayer(Formatter.commandError(Level.SEVERE, ex.toString()));
		}
	}
	
	private TownSettingCollection getPermNode(String node) throws CommandException
	{
		TownSettingCollection set = null;
		if (node.equalsIgnoreCase(Term.TownadmCmdPermArgsServer.toString()))
			set = MyTown.instance.serverSettings;
		else if (node.equalsIgnoreCase(Term.TownadmCmdPermArgsWild.toString()))
			set = MyTown.instance.serverWildSettings;
		else if (node.toLowerCase().startsWith(Term.TownadmCmdPermArgsWild2.toString().toLowerCase()))
		{
			int dim = Integer.parseInt(node.substring(Term.TownadmCmdPermArgsWild2.toString().length()));
			set = MyTown.instance.getWorldWildSettings(dim);
		}
		else
			throw new CommandException(Term.ErrPermSettingCollectionNotFound, node);
		
		return set;
	}

	private void showPermissions(ICommandSender sender, String node) throws CommandException
	{
		TownSettingCollection set = getPermNode(node);
		
		String title = "";
		if (node.equalsIgnoreCase(Term.TownadmCmdPermArgsServer.toString()))
			title = "the server (default for towns)";
		else if (node.equalsIgnoreCase(Term.TownadmCmdPermArgsWild.toString()))
			title = "the wild (default for world wilds)";
		else if (node.toLowerCase().startsWith(Term.TownadmCmdPermArgsWild2.toString().toLowerCase()))
		{
			String dim = node.substring(Term.TownadmCmdPermArgsWild2.toString().length());
			title = "the wild in dimension " + dim;
		}
		
		set.show(sender, title);
	}
	
	private void flushPermissions(ICommandSender sender, String node) throws CommandException
	{
		TownSettingCollection set = getPermNode(node);
		if (set.childs.size() < 1)
			throw new CommandException(Term.ErrPermNoChilds);
		
		set.forceChildsToInherit();
		sender.sendChatToPlayer(Term.PermForced.toString(node));
	}
	
	private void setPermissions(ICommandSender sender, String node, String key, String val) throws CommandException
	{
		TownSettingCollection set = getPermNode(node);

		set.setValue(key, val);
		
		showPermissions(sender, node);
		sender.sendChatToPlayer(Term.PermSetDone.toString(key, node));
	}
}
