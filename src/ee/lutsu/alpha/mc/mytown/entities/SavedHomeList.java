package ee.lutsu.alpha.mc.mytown.entities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.Term;

public class SavedHomeList extends ArrayList<SavedHome>
{
	public Resident owner;
	
	public SavedHomeList(Resident owner)
	{
		this.owner = owner;
	}
	
	public void deserialize(String str)
	{
		this.clear();
		
		if (str == null || str.trim().length() < 1)
			return;
		
		String[] a = str.split("\\|");
		
		for (String b : a)
			add(SavedHome.deserialize(b));
	}
	
	public String serialize()
	{
		List<String> ret = Lists.newArrayList();
		for (SavedHome h : this)
			ret.add(h.serialize());
		
		return Joiner.on("|").join(ret);
	}
	
	public String getHomeName(String name)
	{
		if (name == null || name.trim().length() < 1)
			return "default";
		
		return name.replace('/', '_').replace('|', '_').replace(' ', '_');
	}
	
	public void set(String name, Entity pos) throws CommandException
	{
		name = getHomeName(name);

		SavedHome h = null;
		for (SavedHome a : this)
			if (a.name.equalsIgnoreCase(name))
				h = a;
		
		if (h == null)
		{
			assertCanAddHome();
			add(new SavedHome(name, pos));
		}
		else
			h.reset(pos);
		
		save();
	}
	
	public void delete(String name) throws CommandException
	{
		name = getHomeName(name);
		
		SavedHome h = null;
		for (SavedHome a : this)
			if (a.name.equalsIgnoreCase(name))
				h = a;
		
		if (h == null)
			throw new CommandException(Term.HomeCmdNoHomeByName);
		
		remove(h);
		save();
	}
	
	public void assertCanAddHome() throws CommandException
	{
		// implement limits
	}
	
	public void save()
	{
		owner.save();
	}
}
