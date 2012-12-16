package ee.lutsu.alpha.mc.mytown.Entities;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import ee.lutsu.alpha.mc.mytown.CommandException;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Term;

public class Nation 
{
	public static int nationAddsBlocks = 32;
	public static int nationAddsBlocksPerResident = 0;
	
	private List<Town> towns = new ArrayList<Town>();
	private int id = 0;
	private String name;
	private Town capital;
	private int extraBlocks = 0;
	
	public String name() { return name; }
	public void setName(String v) { name = v; save(); }
	
	public Town capital(){ return capital; }
	public void setCapital(Town t){ capital = t; save(); }
	
	public int extraBlocks() { return extraBlocks; }
	public void setExtraBlocks(int val) { extraBlocks = val; save(); }
	
	public List<Town> towns(){ return towns; }
	
	public int id() { return id; }
	public void setId(int val) { id = val; }
	
	protected Nation() { }
	public Nation(String pName, Town pCapital) throws CommandException
	{
		checkName(pName);
		
		if (pCapital.nation() != null)
			throw new CommandException(Term.TownErrAlreadyInNation);
		
		name = pName;
		capital = pCapital;
		towns.add(capital);
		
		save();
		MyTownDatasource.instance.addNation(this);
	}
	
	public static void checkName(String name) throws CommandException
	{
		
	}
	
	public void save()
	{
		MyTownDatasource.instance.saveNation(this);
	}
	
	public static Nation sqlLoad(int id, String name, int capital, String pTowns, String extra)
	{
		Nation n = new Nation();
		n.id = id;
		n.name = name;
		
		if (pTowns != null && pTowns.trim().length() > 0)
		{
			for (String town : pTowns.trim().split(";"))
			{
				Town t = MyTownDatasource.instance.getTown(Integer.parseInt(town));
				
				if (t != null)
				{
					t.sqlSetExtraBlocks(t.extraBlocks() + nationAddsBlocks + nationAddsBlocksPerResident * t.residents().size());
					
					t.setNation(n);
					n.towns.add(t);
				}
			}
		}
		
		if (capital > 0)
		{
			Town t = MyTownDatasource.instance.getTown(capital);
			
			if (t != null)
				n.capital = t;
		}
		
		// handle extra
		if (extra != null && extra.trim().length() > 0)
		{
			String[] exSp = extra.split(";");
			for (String ex : exSp)
			{
				String[] sp = ex.split(":");
				
				if (sp[0].equals("eb"))
					n.extraBlocks = Integer.parseInt(sp[1]);
			}
		}
		
		return n;
	}
	
	public String serializeExtra()
	{
		List<String> ex = new ArrayList<String>();
		
		if (extraBlocks != 0)
			ex.add("eb:" + String.valueOf(extraBlocks));
		
		return Joiner.on(";").join(ex);
	}
}
