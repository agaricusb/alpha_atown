package ee.lutsu.alpha.mc.mytown;

import java.util.ArrayList;
import java.util.List;

import ru.tehkode.permissions.bukkit.PermissionsEx;

public class GroupManager 
{
	public static GroupManager instance = new GroupManager();
	private static int pexOn = 0;
	
	private boolean pexAvailable()
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

	public String getPrefix(String player, String world)
	{
		if (!pexAvailable())
			return "";

		PermissionsEx pex = PermissionsEx.instance;
		return pex.prefix(player, world);
	}
	
	public String getPostfix(String player, String world)
	{
		if (!pexAvailable())
			return "";
		
		PermissionsEx pex = PermissionsEx.instance;
		return pex.suffix(player, world);
	}
}
