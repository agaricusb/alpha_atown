package ee.lutsu.alpha.mc.mytown;

import net.minecraft.command.ICommandSender;

public class NoAccessException extends Exception
{
	public String node;
	public ICommandSender executor;
	
	public NoAccessException(ICommandSender executor, String node)
	{
		this.node = node;
		this.executor = executor;
	}
	
	@Override
	public String toString()
	{
		return Formatter.dollarToColorPrefix(Permissions.getOption(executor, "permission-denied-" + node, Term.ErrCannotAccessCommand.toString()));
	}
}
