package ee.lutsu.alpha.mc.mytown.commands;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;

public class CmdMyTown extends CommandBase
{
	@Override
	public String getCommandName() 
	{
		return Term.TownCommand.toString();
	}
	
	@Override
    public List getCommandAliases()
    {
		return Arrays.asList(Term.TownCommandAliases.toString().split(" "));
    }
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender cs)
	{
		return Permissions.canAccess(cs, "mytown.cmd");
	}
	
	@Override
    public String getCommandUsage(ICommandSender par1ICommandSender)
    {
		return "/" + getCommandName();
    }

	@Override
	public void processCommand(ICommandSender var1, String[] var2) 
	{
		try
		{
			// all
			MyTownEveryone.handleCommand(var1, var2);
			
			// in town
			MyTownResident.handleCommand(var1, var2);
			MyTownAssistant.handleCommand(var1, var2);
			MyTownMayor.handleCommand(var1, var2);
			MyTownNation.handleCommand(var1, var2);
			
			// not in town
			MyTownNonResident.handleCommand(var1, var2);
		}
		catch(NumberFormatException ex)
		{
			var1.sendChatToPlayer(Formatter.commandError(Level.WARNING, Term.TownErrCmdNumberFormatException.toString()));
		}
		catch(CommandException ex)
		{
			var1.sendChatToPlayer(Formatter.commandError(Level.WARNING, ex.errorCode.toString(ex.args)));
		}
		catch(Throwable ex)
		{
			Log.log(Level.WARNING, String.format("Command execution error by %s", var1), ex);
			var1.sendChatToPlayer(Formatter.commandError(Level.SEVERE, ex.toString()));
		}
	}

}
