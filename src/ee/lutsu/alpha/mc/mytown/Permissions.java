package ee.lutsu.alpha.mc.mytown;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ru.tehkode.permissions.IPermissions;

public class Permissions
{
	private static int pexOn = 0;
	private static IPermissions pex = null;
	
	private static boolean pexAvailable()
	{
		if (pexOn == 0)
		{
			for (ModContainer cont : Loader.instance().getModList())
			{
				if (cont.getModId().equalsIgnoreCase("PermissionsEx"))
				{
					if (cont.getMod() instanceof IPermissions)
						pex = (IPermissions)cont.getMod();
					
					break;
				}
			}
			pexOn = pex == null ? 2 : 1;
		}
		
		return pexOn == 1;
	}
	
	public static boolean canAccess(EntityPlayer name, String node)
	{
		return canAccess(name.username, String.valueOf(name.dimension), node);
	}
	
	public static boolean canAccess(ICommandSender name, String node)
	{
		if (!(name instanceof EntityPlayer))
			return true;
		else
		{
			EntityPlayer pl = (EntityPlayer)name;
			return canAccess(pl.username, String.valueOf(pl.dimension), node);
		}
	}
	
	public static boolean canAccess(Resident name, String node)
	{
		return canAccess(name.name(), name.onlinePlayer != null ? String.valueOf(name.onlinePlayer.dimension) : "0", node);
	}
	
	public static boolean canAccess(String name, String world, String node)
	{
		if (!pexAvailable())
			throw new RuntimeException("PEX not found");
		
		return pex.has(name, node, world);
	}
	
	public static String getPrefix(String player, String world)
	{
		if (!pexAvailable())
			return "";

		return pex.prefix(player, world);
	}
	
	public static String getPostfix(String player, String world)
	{
		if (!pexAvailable())
			return "";

		return pex.suffix(player, world);
	}
}