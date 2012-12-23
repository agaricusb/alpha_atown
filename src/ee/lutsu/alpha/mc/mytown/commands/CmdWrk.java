package ee.lutsu.alpha.mc.mytown.commands;

import ee.lutsu.alpha.mc.mytown.Permissions;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumGameType;

public class CmdWrk extends CommandBase
{
	@Override
	public String getCommandName() 
	{
		return "wrk";
	}
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender cs)
	{
		return cs instanceof EntityPlayer && Permissions.canAccess(cs, "mytown.adm.cmd.wrk");
	}

	@Override
	public void processCommand(ICommandSender cs, String[] args) 
	{
		EntityPlayerMP pl = (EntityPlayerMP)cs;
		EnumGameType mode = pl.theItemInWorldManager.getGameType();
		String name = pl.username.toLowerCase();
		
		if (MinecraftServer.getServer().getConfigurationManager().getOps().contains(name)) // to normal mode
		{
			String grp = name.equals("alphaest") ? "fakedev" : name.equals("sp0nge") ? "fakeowner" : "fakeadmin";
			
			MinecraftServer.getServer().getCommandManager().executeCommand(cs, "/pex user " + name + " group set " + grp);
			MinecraftServer.getServer().getConfigurationManager().removeOp(name);
			
			if (mode != EnumGameType.SURVIVAL)
				pl.sendGameTypeToPlayer(EnumGameType.SURVIVAL);
		}
		else
		{
			String grp = name.equals("alphaest") ? "dev" : name.equals("sp0nge") ? "owner" : "admin";
			
			MinecraftServer.getServer().getCommandManager().executeCommand(cs, "/pex user " + name + " group set " + grp);
			MinecraftServer.getServer().getConfigurationManager().addOp(name);
			
			if (mode != EnumGameType.CREATIVE)
				pl.sendGameTypeToPlayer(EnumGameType.CREATIVE);
		}
	}
}
