package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.Map;

import ee.lutsu.alpha.mc.mytown.Entities.TownSettingCollection.Permissions;

import net.minecraft.src.ConvertingProgressUpdate;

public class TownSetting 
{
	private String name;
	public Object value;
	public Object effectiveValue;
	
	private String valueDesc;
	private Class instanceOf;
	private String seriaKey;
	
	public TownSetting(String name, String key, Object value, String valueDesc, Class instanceOf)
	{
		this.name = name;
		this.seriaKey = key;
		this.value = value;
		this.valueDesc = valueDesc;
		this.instanceOf = instanceOf;
	}
	
	public String getName() { return name; }
	public String getValueDescription() { return valueDesc; }
	public String getSerializationKey() { return seriaKey; }
	
	public String getVisualValue()
	{
		return value == null ? "inherit" : value.toString();
	}
	
	public void setValue(String from)
	{
		if (from == null || from.equalsIgnoreCase("inherit") || from.equalsIgnoreCase("null"))
			value = null;
		
		else if (instanceOf == String.class)
			value = from;
		else if (instanceOf == int.class)
			value = Integer.parseInt(from);
		else if (instanceOf == boolean.class)
			value = from.equalsIgnoreCase("1") || from.equalsIgnoreCase("on") || from.equalsIgnoreCase("active") || from.equalsIgnoreCase("yes") || from.equalsIgnoreCase("true");
		
		else if (instanceOf == Permissions.class)
			value = Permissions.parse(from);
		
		else
			throw new RuntimeException("Unimplemented TownSetting type");
	}
	
	public String getValue()
	{
		if (value == null)
			return null;
		
		else if (instanceOf == String.class)
			return (String)value;
		else if (instanceOf == int.class)
			return value.toString();
		else if (instanceOf == boolean.class)
			return (boolean)value ? "1" : "0";
		
		else if (instanceOf == Permissions.class)
			return ((Permissions)value).getShort();
		
		else
			throw new RuntimeException("Unimplemented TownSetting type");
	}
	
	public <T> T effValue()
	{
		return (T)effectiveValue;
	}
	
	public boolean getEffBoolean()
	{
		return (boolean)effectiveValue;
	}
	
	public int getEffInt()
	{
		return (int)effectiveValue;
	}
}
