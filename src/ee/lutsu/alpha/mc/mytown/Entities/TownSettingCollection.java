package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

/**
 * 
 * Hierarchy:
 * Server
 *  L World
 *     L Town
 *        L Resident
 *           L Plot
 *
 */
public class TownSettingCollection 
{
	public enum Permissions
	{
		None, // First char has to be different
		Enter,
		Loot,
		Access,
		Build;
		
		public String getShort() { return toString().substring(0, 0); }
		
		public static Permissions parse(String str)
		{
			for (Permissions val : values())
			{
				if (val.toString().toLowerCase().startsWith(str.toLowerCase()))
					return val;
			}
			
			return None;
		}
		
		public static String getValuesDesc()
		{
			List<String> vals = new ArrayList<String>();
			for (Permissions val : values())
			{
				vals.add(val.toString());
			}
			return Joiner.on(",").join(vals);
		}
	}
	
	public interface ISettingsSaveHandler
	{
		void save(TownSettingCollection sender, Object tag);
	}
	
	public TownSettingCollection parent;
	public List<TownSettingCollection> childs = new ArrayList<TownSettingCollection>();
	
	public List<TownSetting> settings = new ArrayList<TownSetting>();
	
	public ISettingsSaveHandler saveHandler;
	public Object tag;
	
	public TownSettingCollection()
	{
		this(false);
	}
	
	public TownSettingCollection(boolean isRoot)
	{
		reset(isRoot);
	}
	
	public void setParent(TownSettingCollection col)
	{
		parent = col;
		parent.addChild(this);
	}
	
	protected void addChild(TownSettingCollection col)
	{
		childs.add(col);
	}
	
	protected void removeChild(TownSettingCollection col)
	{
		childs.remove(col);
	}
	
	private TownSetting getSetting(String key)
	{
		for (TownSetting set : settings)
		{
			if (set.getSerializationKey() == key)
				return set;
		}
		
		return null;
	}
	
	public Object getEffValue(String key)
	{
		TownSetting set = getSetting(key);
		if (set == null)
			throw new RuntimeException("Unknown setting");
		
		return set.effectiveValue;
	}
	
	public void forceChildsToInherit()
	{
		for (TownSettingCollection child : childs)
		{
			child.clearValues();
			child.refreshSelf();
			child.forceChildsToInherit();
			child.save();
		}
	}
	
	public void save()
	{
		saveHandler.save(this, tag);
	}
	
	private void refreshSelf()
	{
		for (TownSetting set : settings)
		{
			if (set.value == null)
			{
				if (parent == null)
					throw new RuntimeException("Top value cannot be null : " + set.getSerializationKey());
				
				set.effectiveValue = parent.getEffValue(set.getSerializationKey());
			}
			else
				set.effectiveValue = set.value;
			
			unnest(set);
		}
	}
	
	public void refresh()
	{
		refreshSelf();

		for (TownSettingCollection child : childs)
		{
			child.refresh();
		}
	}
	
	public void deserialize(String val)
	{
		if (val == null || val.equals(""))
			return;
		
		String[] splits = val.split(";");
		for(String line : splits)
		{
			String[] v = line.split(":");
			
			TownSetting set = getSetting(v[0]);
			if (set != null)
				set.setValue(v[1]);
		}
	}
	
	public String serialize()
	{
		List<String> ret = new ArrayList<String>();
		
		for (TownSetting set : settings)
			if (set.value != null)
				ret.add(set.getSerializationKey() + ":" + set.getValue());
		
		return Joiner.on(';').join(ret);
	}
	
	private void clearValues()
	{
		for (TownSetting set : settings)
		{
			set.value = null;
		}
	}
	
	// elements
	public boolean allowPickup;
	
	public void reset(boolean isRoot)
	{
		settings.clear();

		settings.add(new TownSetting("Town member rights", "townperm", Permissions.Loot, "choice:" + Permissions.getValuesDesc(), Permissions.class));
		settings.add(new TownSetting("Nation member rights", "nationperm", Permissions.Enter, "choice:" + Permissions.getValuesDesc(), Permissions.class));
		settings.add(new TownSetting("Outsider rights", "outperm", Permissions.Enter, "choice:" + Permissions.getValuesDesc(), Permissions.class));
		settings.add(new TownSetting("Friend rights", "friendperm", Permissions.Build, "choice:" + Permissions.getValuesDesc(), Permissions.class));
		
		settings.add(new TownSetting("Allow cart interaction", "cartinter", false, "boolean:yes/no", boolean.class));
		settings.add(new TownSetting("Allow stevescarts railers", "steverailer", false, "boolean:yes/no", boolean.class));
		settings.add(new TownSetting("Allow stevescarts miners", "steveminer", false, "boolean:yes/no", boolean.class));
		settings.add(new TownSetting("Allow quarrys,filler,builders", "bcmachines", false, "boolean:yes/no", boolean.class));
		
		if (!isRoot)
			clearValues();
	}
	
	protected void unnest(TownSetting set)
	{
		if (set.getSerializationKey().equals("pickup"))
			allowPickup = set.getEffBoolean();
	}
}
