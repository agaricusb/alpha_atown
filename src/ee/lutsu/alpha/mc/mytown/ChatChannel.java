package ee.lutsu.alpha.mc.mytown;

import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;

public enum ChatChannel
{
	Global("Global", "G", "§f"),
	Town("Town", "TC", "§a"),
	Local("Local", "L", "§e"),
	Trade("Trade", "TR", "§7"),
	Help("Help", "H", "§b"),
	;
	
	public final String name;
	public final String abbrevation;
	public final String color;
	public static int localChatDistance = 160;
	
	ChatChannel(String name, String abbrevation, String color)
	{
		this.name = name;
		this.abbrevation = abbrevation;
		this.color = color;
	}
	
	public static ChatChannel parse(String ch)
	{
        for (ChatChannel type : values()) {
            if (type.name.equalsIgnoreCase(ch) || type.abbrevation.equalsIgnoreCase(ch)) {
                return type;
            }
        }
        return ChatChannel.Global;
	}
}