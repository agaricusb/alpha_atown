package ee.lutsu.alpha.mc.mytown.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Entities.Resident;
import ee.lutsu.alpha.mc.mytown.Entities.Town;
import ee.lutsu.alpha.mc.mytown.Entities.TownBlock;
import ee.lutsu.alpha.mc.mytown.Entities.Resident.Rank;

public abstract class MyTownDB extends Database {

	public boolean loaded = false;
	public int dbVersion = 0;
	private Object lock = new Object();
	public static DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
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
    	update_13_12_2012();
    }
    
    private void update_21_11_2012() throws SQLException
    {
    	if (dbVersion > 0)
    		return;
    	
		PreparedStatement statement = prepare("alter table " + prefix + "towns ADD Extra varchar(2000) null");      

		statement.executeUpdate();
		
		dbVersion++;
    }
    
    private void update_13_12_2012() throws SQLException
    {
    	if (dbVersion > 1)
    		return;
    	
    	Table residents = new Table(this, "residents");
    	{
    		residents.add("Id", "INTEGER", true, true);
    		residents.add("Name", "VARCHAR(255)");
    		residents.add("Town", "INTEGER");
    		residents.add("Rank", "VARCHAR(255)");
    		residents.add("Channel", "VARCHAR(255)");
    		residents.add("Created", "VARCHAR(255)");
    		residents.add("LastLogin", "VARCHAR(255)");
    		residents.add("Extra", "TEXT");
    	}
    	residents.execute();

		PreparedStatement statementTown = prepare("SELECT * FROM " + prefix + "towns"); 
		ResultSet setTown = statementTown.executeQuery();
		
		HashMap<Integer, String> towns = new HashMap<Integer, String>();
		while (setTown.next())
			towns.put(setTown.getInt("Id"), setTown.getString("Residents"));
		setTown.close();
		
		for(Entry<Integer, String> town : towns.entrySet())
		{
			int tid = town.getKey();
			String res = town.getValue();
			if (res != null && res != "")
			{
				for(String split : res.split(" "))
				{
					if (split.trim().length() > 0)
					{
						String[] opt = split.trim().split(";");
	
						String rName = opt[0];
						Rank rRank = Rank.parse(opt[1]);
						ChatChannel rChannel = ChatChannel.parse(opt[2]);
						
						PreparedStatement statement = prepare("DELETE FROM " + prefix + "residents where Name = ?");      
						statement.setString(1, rName);
						statement.executeUpdate();
						
		    			statement = prepare("INSERT INTO " + prefix + "residents (Name, Town, Rank, Channel, Created, LastLogin, Extra) VALUES (?, ?, ?, ?, ?, ?, ?)");      
		    			statement.setString(1, rName);
		    			statement.setInt(2, tid);
		    			statement.setString(3, rRank.toString());
		    			statement.setString(4, rChannel.toString());
		    			statement.setString(5, iso8601Format.format(new Date(System.currentTimeMillis())));
		    			statement.setString(6, iso8601Format.format(new Date(System.currentTimeMillis())));
		    			statement.setString(7, "");
	
		    			statement.executeUpdate();
					}
				}
			}
		}
		
		PreparedStatement statement = prepare("UPDATE " + prefix + "towns SET Residents = NULL");      
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
	    			
	    			statement = prepare("UPDATE " + prefix + "residents SET Town = 0, Rank = ? WHERE Town = ?");      
	    			statement.setString(1, Rank.Resident.toString());
	    			statement.setInt(2, town.id());
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
    
    public void saveResident(Resident res)
    {
    	synchronized(lock)
    	{
    		try 
    		{
    			if (res.id() > 0)
    			{
	    			PreparedStatement statement = prepare("UPDATE " + prefix + "residents SET Name = ?, Town = ?, Rank = ?, Channel = ?, LastLogin = ?, Extra = ? WHERE id = ?");      
	    			statement.setString(1, res.name());
	    			statement.setInt(2, res.town() == null ? 0 : res.town().id());
	    			statement.setString(3, res.rank().toString());
	    			statement.setString(4, res.activeChannel.toString());
	    			statement.setString(5, iso8601Format.format(res.lastLogin()));
	    			statement.setString(6, res.extraData());
	    			
	    			statement.setInt(7, res.id());
	    			statement.executeUpdate();
    			}
    			else
    			{
	    			PreparedStatement statement = prepare("INSERT INTO " + prefix + "residents (Name, Town, Rank, Channel, Created, LastLogin, Extra) VALUES (?, ?, ?, ?, ?, ?, ?)", true);      
	    			statement.setString(1, res.name());
	    			statement.setInt(2, res.town() == null ? 0 : res.town().id());
	    			statement.setString(3, res.rank().toString());
	    			statement.setString(4, res.activeChannel.toString());
	    			statement.setString(5, iso8601Format.format(res.created()));
	    			statement.setString(6, iso8601Format.format(res.lastLogin()));
	    			statement.setString(7, res.extraData());

	    			statement.executeUpdate();
	    			
	    			ResultSet rs = statement.getGeneratedKeys();
	    			if (!rs.next())
	    				throw new RuntimeException("Id wasn't returned for new resident " + res.name());

	    			res.setId(rs.getInt(1));
    			}
    		} 
    		catch (Exception e) 
    		{ 
    			e.printStackTrace(); 
    			throw new RuntimeException("Error in town saving", e);
    		}
    	}
    }
    
    public void saveTown(Town town)
    {
    	StringBuilder blocks = new StringBuilder();
    	
    	int i = 0;
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
	    			PreparedStatement statement = prepare("UPDATE " + prefix + "towns SET Name = ?, ExtraBlocks = ?, Blocks = ?, Extra = ? WHERE id = ?");      
	    			statement.setString(1, town.name());
	    			statement.setInt(2, town.extraBlocks());
	    			statement.setString(3, blocks.toString());
	    			statement.setString(4, town.serializeExtra());
	    			
	    			statement.setInt(5, town.id());
	    			statement.executeUpdate();
    			}
    			else
    			{
	    			PreparedStatement statement = prepare("INSERT INTO " + prefix + "towns (Name, ExtraBlocks, Blocks, Extra) VALUES (?, ?, ?, ?)", true);      
	    			statement.setString(1, town.name());
	    			statement.setInt(2, town.extraBlocks());
	    			statement.setString(3, blocks.toString());
	    			statement.setString(4, town.serializeExtra());

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
    
    public abstract Town getTown(int id);
    
    public List<Resident> loadResidents()
    {
		synchronized(lock)
		{
			ResultSet set = null;
			List<Resident> residents = new ArrayList<Resident>();
			try
			{
				PreparedStatement statement = prepare("SELECT * FROM " + prefix + "residents"); 
				set = statement.executeQuery();
				
				while (set.next())
				{
					int tid = set.getInt("Town");
					Town town = tid > 0 ? getTown(tid) : null;
					
					residents.add(Resident.loadFromDB(
							set.getInt("Id"), 
							set.getString("Name"), 
							town, 
							Rank.parse(set.getString("Rank")), 
							ChatChannel.parse(set.getString("Channel")), 
							iso8601Format.parse(set.getString("Created")), 
							iso8601Format.parse(set.getString("LastLogin")), 
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
			
			return residents;
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
	
	public Town loadFromSQL(int pId, String pName, int pExtraBlocks, String pBlocks, String pExtra)
	{
		List<TownBlock> blocks = new ArrayList<TownBlock>();

		if (pBlocks != null && pBlocks != "")
		{
			for(String split : pBlocks.split(" "))
			{
				if (split.trim().length() > 0)
					blocks.add(TownBlock.deserialize(split));
			}
		}
		
		return new Town(pId, pName, pExtraBlocks, blocks, pExtra);
	}
}
