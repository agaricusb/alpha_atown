package ee.lutsu.alpha.mc.mytown;

import java.util.logging.Level;

import net.minecraft.src.EntityPlayer;

import ee.lutsu.alpha.mc.mytown.Entities.Resident;

public class Formatter 
{
	public static boolean formatChat = true;
	
	public static String formatLevel(Level lvl)
	{
		if (lvl == Level.SEVERE)
			return "§4" + Term.SevereLevel;
		else if (lvl == Level.SEVERE)
			return "§6" + Term.WarningLevel;
		else
			return "§a" + Term.InfoLevel;
	}
	
	public static String townNotification(Level lvl, String msg)
	{
		return String.format("[%s%s§f][%s§f] %s%s", ChatChannel.Town.color, ChatChannel.Town.abbrevation, formatLevel(lvl), ChatChannel.Town.color, msg);
	}
	
	public static String formatCommand(String cmd, String args, String info, String color)
	{
		if (color == null)
			color = "f";

		return String.format("§%s/%s %s §7%s §%s- %s", color, Term.TownCommand, cmd, args, color, info);
	}
	
	public static String formatAdminCommand(String cmd, String args, String info, String color)
	{
		if (color == null)
			color = "f";

		return String.format("§%s/%s %s §7%s §%s- %s", color, Term.TownAdmCommand, cmd, args, color, info);
	}
	
	public static String commandError(Level lvl, String msg)
	{
		return String.format("[%s§f] %s", formatLevel(lvl), msg);
	}
	
	public static String formatChat(Resident res, String line, String forgeFormatted, ChatChannel channel)
	{
		if (!formatChat)
			return forgeFormatted;

		return Term.ChatFormat.toString()
				.replace("$color$", channel.color)
				.replace("$channel$", channel.abbrevation)
				.replace("$name$", res.name())
				.replace("$msg$", line)
				.replace("$prefix$", res.prefix())
				.replace("$postfix$", res.postfix());
	}
	
	public static String formatResidentName(Resident r)
	{
		if (r.isOnline())
			return String.format("[%s]", r.name());
		else
			return String.format("%s", r.name());
	}
}