package ee.lutsu.alpha.mc.mytown;

import net.minecraft.src.EntityPlayer;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class Permissions
{
	private static int pexOn = 0;
	
	private static boolean pexAvailable()
	{
		if (pexOn == 0)
		{
			try
			{
				PermissionsEx.class.getName();
				pexOn = 1;
			}
			catch(Throwable ex)
			{
				pexOn = 2;
			}
		}
		
		return pexOn == 1;
	}
	
	public static boolean canAccess(EntityPlayer name, String node)
	{
		return canAccess(name.username, String.valueOf(name.dimension), node);
	}
	
	public static boolean canAccess(Resident name, String node)
	{
		return canAccess(name.name(), name.onlinePlayer != null ? String.valueOf(name.onlinePlayer.dimension) : "0", node);
	}
	
	public static boolean canAccess(String name, String world, String node)
	{
		if (!pexAvailable())
			throw new RuntimeException("PEX not found");
		
		return PermissionsEx.instance.has(name, node, world);
	}
}
