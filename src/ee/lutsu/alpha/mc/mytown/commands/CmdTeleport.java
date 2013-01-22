package ee.lutsu.alpha.mc.mytown.commands;

import ee.lutsu.alpha.mc.mytown.Permissions;
import net.minecraft.command.CommandServerTp;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CmdTeleport extends CommandServerTp 
{
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender cs)
	{
		return (cs instanceof MinecraftServer) || (cs instanceof EntityPlayer && (Permissions.canAccess(cs, "mytown.adm.cmd.tp") || MinecraftServer.getServer().getConfigurationManager().getOps().contains(cs.getCommandSenderName().toLowerCase())));
	}
	
	@Override
    public void processCommand(ICommandSender par1ICommandSender, String[] par2ArrayOfStr)
    {
        if (par2ArrayOfStr.length < 1)
        {
            throw new WrongUsageException("commands.tp.usage", new Object[0]);
        }
        else
        {
            EntityPlayerMP self;

            // /tp [self] <target player>
            // /tp [self] <x> <y> <z>
            if (par2ArrayOfStr.length != 2 && par2ArrayOfStr.length != 4)
                self = getCommandSenderAsPlayer(par1ICommandSender);
            else
            {
                self = func_82359_c(par1ICommandSender, par2ArrayOfStr[0]);
                if (self == null)
                    throw new PlayerNotFoundException();
            }

            if (par2ArrayOfStr.length != 3 && par2ArrayOfStr.length != 4)
            {
                if (par2ArrayOfStr.length == 1 || par2ArrayOfStr.length == 2)
                {
                    EntityPlayerMP targetPlayer = func_82359_c(par1ICommandSender, par2ArrayOfStr[par2ArrayOfStr.length - 1]);

                    if (targetPlayer == null)
                    {
                        throw new PlayerNotFoundException();
                    }

                    if (targetPlayer.worldObj != self.worldObj)
                		MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(self, targetPlayer.dimension);

                    self.playerNetServerHandler.setPlayerLocation(targetPlayer.posX, targetPlayer.posY, targetPlayer.posZ, targetPlayer.rotationYaw, targetPlayer.rotationPitch);
                    notifyAdmins(par1ICommandSender, "commands.tp.success", new Object[] {self.getEntityName(), targetPlayer.getEntityName()});
                }
            }
            else if (self.worldObj != null)
            {
                int var4 = par2ArrayOfStr.length - 3;
                double var5 = func_82368_a(par1ICommandSender, self.posX, par2ArrayOfStr[var4++]);
                double var7 = func_82367_a(par1ICommandSender, self.posY, par2ArrayOfStr[var4++], 0, 0);
                double var9 = func_82368_a(par1ICommandSender, self.posZ, par2ArrayOfStr[var4++]);
                self.setPositionAndUpdate(var5, var7, var9);
                notifyAdmins(par1ICommandSender, "commands.tp.success.coordinates", new Object[] {self.getEntityName(), Double.valueOf(var5), Double.valueOf(var7), Double.valueOf(var9)});
            }
        }
    }


    private double func_82368_a(ICommandSender par1ICommandSender, double par2, String par4Str)
    {
        return this.func_82367_a(par1ICommandSender, par2, par4Str, -30000000, 30000000);
    }

    private double func_82367_a(ICommandSender par1ICommandSender, double par2, String par4Str, int par5, int par6)
    {
        boolean var7 = par4Str.startsWith("~");
        double var8 = var7 ? par2 : 0.0D;

        if (!var7 || par4Str.length() > 1)
        {
            boolean var10 = par4Str.contains(".");

            if (var7)
            {
                par4Str = par4Str.substring(1);
            }

            var8 += func_82363_b(par1ICommandSender, par4Str);

            if (!var10 && !var7)
            {
                var8 += 0.5D;
            }
        }

        if (par5 != 0 || par6 != 0)
        {
            if (var8 < (double)par5)
            {
                throw new NumberInvalidException("commands.generic.double.tooSmall", new Object[] {Double.valueOf(var8), Integer.valueOf(par5)});
            }

            if (var8 > (double)par6)
            {
                throw new NumberInvalidException("commands.generic.double.tooBig", new Object[] {Double.valueOf(var8), Integer.valueOf(par6)});
            }
        }

        return var8;
    }
}
