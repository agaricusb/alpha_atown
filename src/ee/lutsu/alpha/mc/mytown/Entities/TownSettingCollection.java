package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.src.ICommandSender;

import com.google.common.base.Joiner;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Term;

/**
 * 
 * Hierarchy:
 * Server-Wild
 *  L World-Wild
 * Server
 *  L Town
 *     L Resident
 *        L Plot
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
	
	public boolean isWild, isRoot;
	public TownSettingCollection parent;
	public List<TownSettingCollection> childs = new ArrayList<TownSettingCollection>();
	
	public List<TownSetting> settings = new ArrayList<TownSetting>();
	
	public ISettingsSaveHandler saveHandler;
	public Object tag;
	
	public TownSettingCollection()
	{
		this(false, false);
	}
	
	public TownSettingCollection(boolean isRoot, boolean isWild)
	{
		this.isWild = isWild;
		this.isRoot = isRoot;
		reset();
	}
	
	public void setParent(TownSettingCollection col)
	{
		if (parent != null)
			parent.removeChild(this);

		parent = col;
		
		if (parent != null)
		{
			parent.addChild(this);
			refresh();
		}
	}
	
	protected void addChild(TownSettingCollection col)
	{
		childs.add(col);
	}
	
	protected void removeChild(TownSettingCollection col)
	{
		childs.remove(col);
	}
	
	public void unlinkAllDown()
	{
		setParent(null);
		
		for (Object child : childs.toArray())
		{
			((TownSettingCollection)child).unlinkAllDown();
		}
	}
	
	private TownSetting getSetting(String key)
	{
		for (TownSetting set : settings)
		{
			if (set.getSerializationKey().equalsIgnoreCase(key))
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
				{
					if (!isWild)
						throw new RuntimeException("Top value cannot be null : " + set.getSerializationKey());
				}
				else
					set.effectiveValue = parent.getEffValue(set.getSerializationKey());
			}
			else
				set.effectiveValue = set.value;
			
			unnest(set);
		}
	}
	
	public void setValue(String key, String value) throws CommandException
	{
		TownSetting set = getSetting(key);
		if (set == null)
			throw new CommandException(Term.ErrPermSettingNotFound, key);
		
		if (value != null && value.equals("?"))
			throw new CommandException(Term.ErrPermSupportedValues, set.getValueType(), set.getValueDescription());
		
		try
		{
			set.setValue(value);
		}
		catch (Exception e)
		{
			String err =  e.getClass().getSimpleName() + (e.toString() != null ? ": " + e.toString() : "");
			throw new CommandException(Term.ErrPermSupportedValues, err, set.getValueDescription());
		}
		
		refresh();
		save();
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
			if (v.length != 2)
				continue;
			
			TownSetting set = getSetting(v[0]);
			if (set != null)
				set.setValue(v[1]);
		}
		
		refresh();
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
	
	private void clearValuesToWild()
	{
		for (TownSetting set : settings)
		{
			set.value = set.wildValue;
		}
	}
	
	public void show(ICommandSender cs, String title)
	{
		cs.sendChatToPlayer(String.format("§6-- §ePermissions for %s§6 --", title));
		
		for (TownSetting set : settings)
		{
			if (!isWild || set.wildValue != null)
				cs.sendChatToPlayer(String.format("§a%s §2[%s] : %s%s",
					set.getName(),
					set.getSerializationKey(),
					set.value == null ? "§d" : "§c",
					set.getVisualValue()));
		}
	}
	
	// elements
	public Permissions townMemberRights;
	public Permissions nationMemberRights;
	public Permissions outsiderRights;
	public Permissions friendRights;
	
	public boolean allowCartInteraction;
	public boolean allowStevecartsRailers;
	public boolean allowStevecartsMiners;
	public boolean allowBuildcraftMiners;
	
	protected void unnest(TownSetting set)
	{
		if (set.getSerializationKey().equals("town"))
			townMemberRights = set.<Permissions>effValue();
		else if (set.getSerializationKey().equals("nation"))
			nationMemberRights = set.<Permissions>effValue();
		else if (set.getSerializationKey().equals("out"))
			outsiderRights = set.<Permissions>effValue();
		else if (set.getSerializationKey().equals("friend"))
			friendRights = set.<Permissions>effValue();
		
		else if (set.getSerializationKey().equals("carts"))
			allowCartInteraction = set.getEffBoolean();
		else if (set.getSerializationKey().equals("steverailer"))
			allowStevecartsRailers = set.getEffBoolean();
		else if (set.getSerializationKey().equals("steveminer"))
			allowStevecartsMiners = set.getEffBoolean();
		else if (set.getSerializationKey().equals("bc"))
			allowBuildcraftMiners = set.getEffBoolean();
	}
	
	public void reset()
	{
		settings.clear();

		//                             label                             key             default value       wild value          value limitation description                value conversion class
		settings.add(new TownSetting("Town member rights", 				"town", 		Permissions.Loot, 	null, 				"choice:" + Permissions.getValuesDesc(), 	Permissions.class));
		settings.add(new TownSetting("Nation member rights", 			"nation", 		Permissions.Enter, 	null, 				"choice:" + Permissions.getValuesDesc(), 	Permissions.class));
		settings.add(new TownSetting("Outsider rights", 				"out", 			Permissions.Enter, 	Permissions.Build, 	"choice:" + Permissions.getValuesDesc(), 	Permissions.class));
		settings.add(new TownSetting("Friend rights", 					"friend", 		Permissions.Build, 	null, 				"choice:" + Permissions.getValuesDesc(), 	Permissions.class));
		
		settings.add(new TownSetting("Allow cart interaction", 			"carts",	 	false, 				true, 				"boolean:yes/no", 							boolean.class));
		settings.add(new TownSetting("Allow stevescarts railers", 		"steverailer", 	false, 				true, 				"boolean:yes/no", 							boolean.class));
		settings.add(new TownSetting("Allow stevescarts miners", 		"steveminer", 	false, 				true, 				"boolean:yes/no", 							boolean.class));
		settings.add(new TownSetting("Allow quarrys,filler,builders", 	"bc",		 	false, 				true, 				"boolean:yes/no", 							boolean.class));
		
		if (!isRoot)
			clearValues();
		else 
		{
			if (isWild)
				clearValuesToWild();
			
			refresh(); // non-roots will refresh on parent set
		}
	}
}
