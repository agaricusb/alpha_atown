package ee.lutsu.alpha.mc.mytown.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;

public class MyTownDB extends Database {

	public boolean loaded = false;
	public int dbVersion = 0;
	private Object lock = new Object();
	
	@Override
	public void load() 
	{
		if (loaded)
			return;
		
		synchronized(lock)
		{
			try
			{
				tryCreateTables();
				tryDoUpdates();
			} 
			catch (Exception e) 
			{ 
				e.printStackTrace(); 
				throw new RuntimeException("Error in making/updating tables", e);
			}
		}
		
		loaded = true;
	}

    public void tryCreateTables()
    {
    	Table towns = new Table(this, "towns");
    	{
    		towns.add("Id", "INTEGER", true, true);
    		towns.add("Name", "VARCHAR(255)");
    		towns.add("ExtraBlocks", "INTEGER");
    		towns.add("Residents", "TEXT");
    		towns.add("Blocks", "TEXT");
    	}
    	towns.execute();
    }
    
    public void tryDoUpdates() throws SQLException
    {
    	update_21_11_2012();
    }
    
    private void update_21_11_2012() throws SQLException
    {
    	if (dbVersion > 0)
    		return;
    	
		PreparedStatement statement = prepare("alter table " + prefix + "towns ADD Extra varchar(2000) null");      

		statement.executeUpdate();
		
		dbVersion++;
    }
    
    public void deleteTown(Town town)
    {
    	synchronized(lock)
    	{
    		try 
    		{
    			if (town.id() > 0)
    			{
	    			PreparedStatement statement = prepare("DELETE FROM " + prefix + "towns WHERE id = ?");      
	    			statement.setInt(1, town.id());
	    			statement.executeUpdate();
    			}
    		} 
    		catch (Exception e) 
    		{ 
    			e.printStackTrace(); 
    			throw new RuntimeException("Error in town deleting", e);
    		}
    	}
    }
    
    public void saveTown(Town town)
    {
    	StringBuilder residents = new StringBuilder();
    	StringBuilder blocks = new StringBuilder();
    	
    	int i = 0;
    	for(Resident res : town.residents())
    	{
    		res.serialize(residents);

    		if (++i < town.residents().size())
    			residents.append(" ");
    	}
    	i = 0;
    	for(TownBlock block : town.blocks())
    	{
    		blocks.append(block.serialize());
    		
    		if (++i < town.blocks().size())
    			blocks.append(" ");
    	}
    	
    	
    	synchronized(lock)
    	{
    		try 
    		{
    			if (town.id() > 0)
    			{
	    			PreparedStatement statement = prepare("UPDATE " + prefix + "towns SET Name = ?, ExtraBlocks = ?, Residents = ?, Blocks = ?, Extra = ? WHERE id = ?");      
	    			statement.setString(1, town.name());
	    			statement.setInt(2, town.extraBlocks());
	    			statement.setString(3, residents.toString());
	    			statement.setString(4, blocks.toString());
	    			statement.setString(5, town.serializeExtra());
	    			
	    			statement.setInt(6, town.id());
	    			statement.executeUpdate();
    			}
    			else
    			{
	    			PreparedStatement statement = prepare("INSERT INTO " + prefix + "towns (Name, ExtraBlocks, Residents, Blocks, Extra) VALUES (?, ?, ?, ?, ?)", true);      
	    			statement.setString(1, town.name());
	    			statement.setInt(2, town.extraBlocks());
	    			statement.setString(3, residents.toString());
	    			statement.setString(4, blocks.toString());
	    			statement.setString(5, town.serializeExtra());

	    			statement.executeUpdate();

	    			ResultSet rs = statement.getGeneratedKeys();
	    			if (!rs.next())
	    				throw new RuntimeException("Id wasn't returned for new town " + town.name());

	    			town.setId(rs.getInt(1));
    			}
    		} 
    		catch (Exception e) 
    		{ 
    			e.printStackTrace(); 
    			throw new RuntimeException("Error in town saving", e);
    		}
    	}
    }
    
	public List<Town> loadTowns()
	{
		synchronized(lock)
		{
			ResultSet set = null;
			List<Town> towns = new ArrayList<Town>();
			try
			{
				PreparedStatement statement = prepare("SELECT * FROM " + prefix + "towns"); 
				set = statement.executeQuery();
				
				while (set.next())
				{
					towns.add(loadFromSQL(
							set.getInt("Id"), 
							set.getString("Name"), 
							set.getInt("ExtraBlocks"),
							set.getString("Residents"),
							set.getString("Blocks"),
							set.getString("Extra")));
				}
			} 
			catch (Exception e)
			{      
				printException(e);    
			}
			finally
			{
				if (set != null)
				{
					try
					{
						set.close();
					}
					catch(Exception e) { }
				}
			}
			
			return towns;
		}
	}
	
	public Town loadFromSQL(int pId, String pName, int pExtraBlocks, String pResidents, String pBlocks, String pExtra)
	{
		List<Resident> residents = new ArrayList<Resident>();
		List<TownBlock> blocks = new ArrayList<TownBlock>();
		
		if (pResidents != null && pResidents != "")
		{
			for(String split : pResidents.split(" "))
			{
				if (split.trim().length() > 0)
					residents.add(Resident.deserialize(split));
			}
		}
		
		if (pBlocks != null && pBlocks != "")
		{
			for(String split : pBlocks.split(" "))
			{
				if (split.trim().length() > 0)
					blocks.add(TownBlock.deserialize(split));
			}
		}
		
		return new Town(pId, pName, pExtraBlocks, residents, blocks, pExtra);
	}
}
