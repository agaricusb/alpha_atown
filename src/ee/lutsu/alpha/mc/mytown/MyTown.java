package ee.lutsu.alpha.mc.mytown;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import ee.lutsu.alpha.mc.mytown.commands.*;
import ee.lutsu.alpha.mc.mytown.entities.ItemIdRange;
import ee.lutsu.alpha.mc.mytown.entities.Nation;
import ee.lutsu.alpha.mc.mytown.entities.Resident;
import ee.lutsu.alpha.mc.mytown.entities.Town;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.ISettingsSaveHandler;
import ee.lutsu.alpha.mc.mytown.event.*;
import ee.lutsu.alpha.mc.mytown.event.prot.BuildCraft;
import ee.lutsu.alpha.mc.mytown.event.prot.Creeper;
import ee.lutsu.alpha.mc.mytown.event.prot.MiningLaser;
import ee.lutsu.alpha.mc.mytown.event.prot.Mobs;
import ee.lutsu.alpha.mc.mytown.event.prot.PortalGun;
import ee.lutsu.alpha.mc.mytown.event.prot.RedPower;
import ee.lutsu.alpha.mc.mytown.event.prot.SteveCarts;
import ee.lutsu.alpha.mc.mytown.event.prot.ThaumCraft;
import ee.lutsu.alpha.mc.mytown.sql.Database;
import ee.lutsu.alpha.mc.mytown.sql.MyTownDB;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;

@Mod(
        modid = "MyTown",
        name = "My Town",
        version = "1.0.0"
)
@NetworkMod(
        clientSideRequired = false,
        serverSideRequired = true
)
public class MyTown
{
	public static String MOD_NAME = "MyTown";
	public static String CONFIG_FOLDER = "config/MyTown/";
	public static String LIB_FOLDER = CONFIG_FOLDER +"lib/";
	public static String CONFIG_FILE = CONFIG_FOLDER + "MyTown.cfg";
	
	public TownSettingCollection serverWildSettings = new TownSettingCollection(true, true);
	public TownSettingCollection serverSettings = new TownSettingCollection(true, false);
	public Map<Integer, TownSettingCollection> worldWildSettings = new HashMap<Integer, TownSettingCollection>();
	public LinkedList<ItemIdRange> carts = null, safeItems = null;
	
    @Mod.Instance("MyTown")
    public static MyTown instance;
    public Configuration config = new Configuration(new File(CONFIG_FILE));

    @Mod.PreInit
    public void preInit(FMLPreInitializationEvent ev)
    {
        loadConfig();
    }

    @Mod.Init
    public void load(FMLInitializationEvent var1)
    {
    }

    @Mod.ServerStarted
    public void modsLoaded(FMLServerStartedEvent var1)
    {
    	int dbVer = MyTownDatasource.instance.dbVersion;
    	try
    	{
    		MyTownDatasource.instance.init();
    	}
    	catch(Exception ex)
    	{
    		throw new RuntimeException(ex.getMessage(), ex);
    	}
    	finally
    	{
    		if (dbVer != MyTownDatasource.instance.dbVersion)
    		{
    			Configuration config = new Configuration(new File(CONFIG_FILE));
    	        Property prop = config.get("Database", "Version", 0);
    	        prop.value = Integer.toString(MyTownDatasource.instance.dbVersion);
    	        config.save();
    		}
    	}
    	
    	PlayerEvents events = new PlayerEvents();
    	MinecraftForge.EVENT_BUS.register(events);
    	GameRegistry.registerPlayerTracker(events);
    	MinecraftForge.EVENT_BUS.register(ProtectionEvents.instance);
    	TickRegistry.registerTickHandler(ProtectionEvents.instance, Side.SERVER);
    	MinecraftForge.EVENT_BUS.register(WorldEvents.instance);
    	
    	ServerCommandManager mgr = (ServerCommandManager)MinecraftServer.getServer().getCommandManager();
    	mgr.registerCommand(new CmdMyTown());
    	mgr.registerCommand(new CmdMyTownAdmin());
    	mgr.registerCommand(new CmdChannel());
    	mgr.registerCommand(new CmdGamemode());
    	mgr.registerCommand(new CmdWrk());
    	mgr.registerCommand(new CmdSpawn());
    	
		for(ChatChannel c : ChatChannel.values())
			mgr.registerCommand(new CmdChat(c));
    }

    public void saveConfig()
    {
    	
    }
    
    public void loadConfig()
    {
        try
        {
            config.load();
            
            loadGeneralConfigs(config);
            loadDatabaseConfigs(config);
            loadChatConfigs(config);
            loadExtraProtectionConfig(config);
            loadPerms(config);
        }
        catch (Exception var8)
        {
            FMLLog.log(Level.SEVERE, var8, MOD_NAME + " was unable to load it\'s configuration successfully", new Object[0]);
            throw new RuntimeException(var8);
        }
        finally
        {
            config.save(); // re-save to add the missing configuration variables
        }
    }
    
    public void reload()
    {
    	loadConfig();
    	BuildCraft.instance.reload(); // flushes the checked list
    	
    	try
    	{
    		MyTownDatasource.instance.init();
    	}
    	catch(Exception ex)
    	{
    		throw new RuntimeException(ex.getMessage(), ex);
    	}
    }
    
    private void loadGeneralConfigs(Configuration config) throws IOException
    {
    	Property prop; 
    	
        prop = config.get("General", "Translations", "");
        prop.comment = "Filename in config folder with the term translations";
        
        if (prop.value != null && !prop.value.equals(""))
        	TermTranslator.load(new File(CONFIG_FOLDER + prop.value), "custom", true);
        
        prop = config.get("General", "IgnoreOps", true);
        prop.comment = "Should ops be bypassed from protections";
        Resident.ignoreOps = prop.getBoolean(true);
        
        /*
        prop = config.get("General", "BlocksPerResident", 16);
        prop.comment = "How many town block each resident gives";
        Town.perResidentBlocks = prop.getInt(16);*/
        
        prop = config.get("General", "NationAddsBlocks", 0);
        prop.comment = "How many town blocks the town gets for being in a nation";
        Nation.nationAddsBlocks = prop.getInt(0);
        
        prop = config.get("General", "NationAddsBlocksPerResident", 0);
        prop.comment = "How many town blocks each resident gives if the town is in a nation";
        Nation.nationAddsBlocksPerResident = prop.getInt(0);
        
        prop = config.get("General", "MinDistanceFromAnotherTown", 50);
        prop.comment = "How many blocks(chunks) apart have the town blocks be";
        Town.minDistanceFromOtherTown = prop.getInt(50);
        
        prop = config.get("General", "AllowTownMemberPvp", false);
        prop.comment = "First check. Can one town member hit a member of the same town? Anywhere. Also called friendlyfire";
        Resident.allowMemberToMemberPvp = prop.getBoolean(false);
        
        prop = config.get("General", "AllowPvpInTown", false);
        prop.comment = "Second check. Can anyone hit anyone in town? For PVP only. Does NOT turn friendly fire on";
        Town.allowFullPvp = prop.getBoolean(false);
        
        prop = config.get("General", "AllowMemberKillNonMember", true);
        prop.comment = "Third check. Can a member of the town kill someone who doesn't belong to his town?";
        Town.allowMemberToForeignPvp = prop.getBoolean(true);
        
        List items = Arrays.asList(Item.itemsList);
        prop = config.get("General", "CartItemIds", String.valueOf(items.indexOf(Item.minecartEmpty)) + ";" + String.valueOf(items.indexOf(Item.minecartPowered)) + ";" + String.valueOf(items.indexOf(Item.minecartCrate)));
        prop.comment = "Defines the cart id's which can be placed on a rail with carts perm on";
        carts = ItemIdRange.parseList(Arrays.asList(prop.value.split(";")));

        prop = config.get("General", "SafeItemIds", "");
        prop.comment = "Defines the item id's which don't do anything on right click (red matter?)";
        safeItems = ItemIdRange.parseList(Arrays.asList(prop.value.split(";")));
    }
    
    private void loadDatabaseConfigs(Configuration config)
    {
        Property prop; 
        
        prop = config.get("Database", "Type", "SQLite");
        prop.comment = "Database type to connect to";
        MyTownDatasource.instance.currentType = Database.Type.matchType(prop.value);
    	
        prop = config.get("Database", "Prefix", "");
        prop.comment = "Table name prefix to use. <pre>_towns etc..";
        MyTownDatasource.instance.prefix = prop.value;
    	
        prop = config.get("Database", "Username", "");
        prop.comment = "Username to use when connecting. Used by MySQL";
        MyTownDatasource.instance.username = prop.value;
    	
        prop = config.get("Database", "Password", "");
        prop.comment = "Password to use when connecting. Used by MySQL";
        MyTownDatasource.instance.password = prop.value;
    	
        prop = config.get("Database", "Host", "");
        prop.comment = "Hostname:Port of the db server. Used by MySQL";
        MyTownDatasource.instance.host = prop.value;
    	
        prop = config.get("Database", "Database", "");
        prop.comment = "The database name. Used by MySQL";
        MyTownDatasource.instance.dbname = prop.value;
    	
        prop = config.get("Database", "Path", CONFIG_FOLDER + "data.db");
        prop.comment = "The database file path. Used by SQLite";
        MyTownDatasource.instance.dbpath = prop.value;
        
        prop = config.get("Database", "Version", 0);
        prop.comment = "The database version. DO NOT CHANGE! It's used internally.";
        MyTownDatasource.instance.dbVersion = prop.getInt();
    }
    
    private void loadChatConfigs(Configuration config)
    {
        Property prop; 
        
        prop = config.get("Chat", "FormatChat", true);
        prop.comment = "Should the chat be formatted";
        Formatter.formatChat = prop.getBoolean(true);
        
        prop = config.get("Chat", "LocalDistance", 160);
        prop.comment = "How many blocks far does the local chat sound";
        ChatChannel.localChatDistance = prop.getInt(160);
        
        prop = config.get("Chat", "MaxChatLength", 32767);
        prop.comment = "How many characters can one chat packet contain. It's global.";
        Packet3Chat.maxChatLength = prop.getInt(32767);
    }
    
    private void loadExtraProtectionConfig(Configuration config)
    {
        Property prop; 
        
        prop = config.get("ProtEx", "Enabled", true);
        prop.comment = "Run the extra protections";
        ProtectionEvents.instance.enabled = prop.getBoolean(true);
        
        prop = config.get("ProtEx", "LaserCheck", false);
        prop.comment = "Check for mining laser bypass";
        MiningLaser.instance.enabled = prop.getBoolean(false);
        
        prop = config.get("ProtEx", "PortalGunCheck", false);
        prop.comment = "Check for portal gun balls flying in foreign towns";
        PortalGun.instance.enabled = prop.getBoolean(false);
        
        prop = config.get("ProtEx", "SteveRailerCheck", false);
        prop.comment = "Check for steve carts with railers";
        SteveCarts.instance.enabled = prop.getBoolean(false);
        
        prop = config.get("ProtEx", "BuildCraftCheck", false);
        prop.comment = "Check for quarrys, fillers, builder";
        BuildCraft.instance.enabled = prop.getBoolean(false);
        
        prop = config.get("ProtEx", "CreeperExplosionCheck", true);
        prop.comment = "Check for creepers if they can explode";
        Creeper.instance.enabled = prop.getBoolean(true);
        
        prop = config.get("ProtEx", "MobPositionCheck", true);
        prop.comment = "Check for mobs if they can be at a protected area";
        Mobs.instance.enabled = prop.getBoolean(true);
        
        prop = config.get("ProtEx", "ThaumCraft", false);
        prop.comment = "Check for ThaumCraft 3 bypasses";
        ThaumCraft.instance.enabled = prop.getBoolean(true);
        
        prop = config.get("ProtEx", "RedPower", false);
        prop.comment = "Check for RedPower 2 bypasses";
        RedPower.instance.enabled = prop.getBoolean(true);
    }
    
    private void loadPerms(Configuration config)
    {
        Property prop; 
        
        prop = config.get("ServerPerms", "Server", "");
        serverSettings.deserialize(prop.value);
        
        serverSettings.saveHandler = new ISettingsSaveHandler()
        {
			public void save(TownSettingCollection sender, Object tag) 
			{
				MyTown.instance.config.get("ServerPerms", "Server", "").value = sender.serialize();
				MyTown.instance.config.save();
			}
        };
        
        prop = config.get("WildPerms", "Server", "");
        serverWildSettings.deserialize(prop.value);
        
        serverWildSettings.saveHandler = new ISettingsSaveHandler()
        {
			public void save(TownSettingCollection sender, Object tag) 
			{
				MyTown.instance.config.get("WildPerms", "Server", "").value = sender.serialize();
				MyTown.instance.config.save();
			}
        };
        
        Map<String, Property> cat = config.categories.get("WildPerms");
        if (cat == null)
        	return;
        
        for (Property p : cat.values())
        {
        	if (!p.getName().startsWith("Dim_"))
        		continue;

    		int dim = Integer.parseInt(p.getName().substring(4));
    		TownSettingCollection set = getWorldWildSettings(dim);
    		set.deserialize(p.value);
        }
    }
    
    public TownSettingCollection getWorldWildSettings(int w)
    {
    	for (Entry<Integer, TownSettingCollection> set : worldWildSettings.entrySet())
    	{
    		if (set.getKey() == w)
    			return set.getValue();
    	}
    	
    	TownSettingCollection set = new TownSettingCollection(false, true);
    	set.tag = new Integer(w);
    	set.setParent(serverWildSettings);
    	set.saveHandler = new ISettingsSaveHandler()
        {
			public void save(TownSettingCollection sender, Object tag) 
			{
				int w = (Integer)tag;
				MyTown.instance.config.get("WildPerms", "Dim_" + String.valueOf(w), "").value = sender.serialize();
				MyTown.instance.config.save();
			}
        };
        
        worldWildSettings.put(w, set);
        
        return set;
    }
}
