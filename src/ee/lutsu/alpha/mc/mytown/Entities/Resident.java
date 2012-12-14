package ee.lutsu.alpha.mc.mytown.Entities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WorldInfo;

public class Resident 
{
	public static boolean ignoreOps = false;
	public static boolean allowMemberToMemberPvp = false;
	
	public enum Rank
	{
		Resident,
		Assistant,
		Mayor;
		
		/**
		 * Gets the rank based on [R, A, M]
		 */
		public static Rank parse(String rank)
		{
            for (Rank type : values()) {
                if (type.toString().toLowerCase().startsWith(rank.toLowerCase())) {
                    return type;
                }
            }
            return Rank.Resident;
		}
	}
	
	public EntityPlayer onlinePlayer;
	
	private String name;
	private Town town;
	private Rank rank = Rank.Resident;
	private long opCheck = 0;
	private boolean isOp = false;
	private int id = 0;
	private Date createdOn;
	private Date lastLoginOn;
	
	public Town location;
	public boolean mapMode = false;
	public Town inviteActiveFrom;
	public ChatChannel activeChannel = ChatChannel.Global;
	public boolean beingBounced = false;
	public List<Resident> friends = new ArrayList<Resident>();
	
	public int prevDimension, prevDimension2;
	public double prevX, prevY, prevZ, prevX2, prevY2, prevZ2;
	public float prevYaw, prevPitch, prevYaw2, prevPitch2;
	
	public boolean firstTick = true;
	public boolean wasfirstTick = true;
	
	public boolean hasTown() {return town != null; }

	
	public void setActiveChannel(ChatChannel ch) { activeChannel = ch; save(); }
	public Town town(){ return town; }
	public void setTown(Town t){ town = t; }
	public Rank rank(){ return rank; }
	public void setRank(Rank r){ rank = r; }
	public String name() { return name; }
	public boolean isOnline() { return onlinePlayer != null && !onlinePlayer.isDead; }
	public int id() { return id; }
	public void setId(int val) { id = val; }
	public Date created() { return createdOn; }
	public Date lastLogin() { return lastLoginOn; }
	public String extraData() { return ""; }
	
	public Resident(String pName)
	{
		name = pName;
		createdOn = new Date(System.currentTimeMillis());
		lastLoginOn = new Date(System.currentTimeMillis());
		
		save();
	}
	
	protected Resident()
	{
	}
	
	public boolean isOp()
	{
		if (opCheck < System.currentTimeMillis())
		{
			isOp = MinecraftServer.getServer().getConfigurationManager().getOps().contains(name.trim().toLowerCase());
			opCheck = System.currentTimeMillis() + 5 * 1000;
		}
		return isOp;
	}
	
	public boolean canInteract(int chunkX, int chunkZ)
	{
		if (ignoreOps && isOp())
			return true;
		
		TownBlock block = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, chunkX, chunkZ);
		
		if (block == null || block.town() == null)
			return true;
		
		return town() == block.town();
	}
	
	public boolean canInteract(int x, int y, int z)
	{
		return canInteract(x >> 4, z >> 4);
	}
	
	public boolean canInteract(Entity e)
	{
		return canInteract(e.chunkCoordX, e.chunkCoordZ);
	}
	
	public boolean canAttack(Entity e)
	{
		if (ignoreOps && isOp())
			return true;
		

		if (e instanceof EntityPlayer)
		{
			// disable friendly fire
			if (!allowMemberToMemberPvp && town() != null && MyTownDatasource.instance.getOrMakeResident((EntityPlayer)e).town() == town())
				return false;
			
			if (Town.allowFullPvp)
				return true;

			TownBlock targetBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, e.chunkCoordX, e.chunkCoordZ);
			if (targetBlock != null && targetBlock.town() != null)
				return Town.allowMemberToForeignPvp && town() == targetBlock.town();
			
			TownBlock sourceBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, onlinePlayer.chunkCoordX, onlinePlayer.chunkCoordZ);
			if (sourceBlock != null && sourceBlock.town() != null)
				return Town.allowMemberToForeignPvp && town() == sourceBlock.town();
			
			return true;
		}
		else
		{
			TownBlock targetBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, e.chunkCoordX, e.chunkCoordZ);
			
			if (targetBlock != null && targetBlock.town() != null)
				return town() == targetBlock.town();
			else
				return true;
		}
	}
	
	public void sendLocationMap(int dim, int cx, int cz)
	{
		int heightRad = 4;
		int widthRad = 9;
		StringBuilder sb = new StringBuilder();
		String c;
		
		onlinePlayer.sendChatToPlayer(Term.TownMapHead.toString());
		for(int z = cz - heightRad; z <= cz + heightRad; z++)
		{
			sb.setLength(0);
			for(int x = cx - widthRad; x <= cx + widthRad; x++)
			{
				TownBlock b = MyTownDatasource.instance.getBlock(dim, x, z);
				
				if (z == cz && x == cx)
					c = b == null || b.town() == null ? "§e_" : b.town() == town ? "§aO" : "§cX";
				else
					c = b == null || b.town() == null ? "§f_" : b.town() == town ? "§2O" : "§4X";
				
				sb.append(c);
			}
			onlinePlayer.sendChatToPlayer(sb.toString());
		}
	}
	
	public String prefix()
	{
		String w = onlinePlayer != null ? String.valueOf(onlinePlayer.dimension) : null;
		return Permissions.getPrefix(name(), w);
	}
	
	public String postfix()
	{
		String w = onlinePlayer != null ? String.valueOf(onlinePlayer.dimension) : null;
		return Permissions.getPostfix(name(), w);
	}
	
	public static Resident loadFromDB(int id, String name, Town town, Rank r, ChatChannel c, Date created, Date lastLogin, String extra)
	{
		Resident res = new Resident();
		res.name = name;
		res.id = id;
		res.town = town;
		res.rank = r;
		res.activeChannel = c;
		res.createdOn = created;
		res.lastLoginOn = lastLogin;
		
		if (town != null)
			town.residents().add(res);

		// split extra

		return res;
	}
	
	public void bounceAway(int prevX, int prevZ, int newX, int newZ)
	{
		if (wasfirstTick)
			return;
		
		if (this.onlinePlayer instanceof EntityPlayerMP)
		{
			if (this.onlinePlayer.dimension != this.prevDimension2)
				MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension((EntityPlayerMP)this.onlinePlayer, this.prevDimension2);

			 this.prevDimension = prevDimension2;
			 this.prevX = prevX2;
			 this.prevY = prevY2;
			 this.prevZ = prevZ2;
			 this.prevYaw = prevYaw2;
			 this.prevPitch = prevPitch2;
			
			if (onlinePlayer.ridingEntity != null)
				onlinePlayer.ridingEntity.moveEntity(this.prevX2, this.prevY2, this.prevZ2);
			
			((EntityPlayerMP)this.onlinePlayer).playerNetServerHandler.setPlayerLocation(this.prevX2, this.prevY2, this.prevZ2, this.prevYaw2, this.prevPitch2);
		}
		else
			throw new RuntimeException("Cannot bounce non multiplayer players");
	}
	
	public void sendToSpawn()
	{
		if (!(onlinePlayer instanceof EntityPlayerMP))
			throw new RuntimeException("Cannot move a non-player");
		
		if (this.onlinePlayer.dimension != 0)
			MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension((EntityPlayerMP)this.onlinePlayer, 0);
		
		ChunkCoordinates c = onlinePlayer.getSpawnChunk();
		
		if (c != null)
			((EntityPlayerMP)onlinePlayer).setPositionAndUpdate(c.posX, c.posY + 1, c.posZ);
		else
		{
			WorldInfo info = onlinePlayer.worldObj.getWorldInfo();
			int y = onlinePlayer.worldObj.getHeightValue(info.getSpawnX(), info.getSpawnZ());
			
			if (info.getSpawnY() > 0 && info.getSpawnY() != onlinePlayer.worldObj.provider.getAverageGroundLevel())
				y = info.getSpawnY();

			((EntityPlayerMP)onlinePlayer).setPositionAndUpdate(info.getSpawnX(), y, info.getSpawnZ());
		}
	}
	
	public void save()
	{
		MyTownDatasource.instance.saveResident(this);
	}
	
	/**
	 * Called by LivingUpdateEvent
	 */
	public void update()
	{
		if (beingBounced)
			return;
					
		wasfirstTick = false;
		
		if (firstTick)
		{
			firstTick = false;
			wasfirstTick = true;
		}
		else
		{
			prevDimension2 = prevDimension;
			prevX2 = prevX;
			prevY2 = prevY;
			prevZ2 = prevZ;
			prevYaw2 = prevYaw;
			prevPitch2 = prevPitch;
			
			prevDimension = onlinePlayer.dimension;
			prevX = onlinePlayer.posX;
			prevY = onlinePlayer.posY;
			prevZ = onlinePlayer.posZ;
			prevYaw = onlinePlayer.rotationYaw;
			prevPitch = onlinePlayer.rotationPitch;
		}
	}
	
	public void sendInfoTo(ICommandSender cs)
	{
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		cs.sendChatToPlayer(Term.ResStatusName.toString(name));
		cs.sendChatToPlayer(Term.ResStatusGeneral1.toString(format.format(createdOn))); 
		cs.sendChatToPlayer(Term.ResStatusGeneral2.toString(isOnline() ? "online" : format.format(lastLoginOn)));
		cs.sendChatToPlayer(Term.ResStatusTown.toString(
			town == null ? "none" : town().name(),
			town == null ? "Loner" : rank.toString()));
	}
	
	public void loggedIn()
	{
		lastLoginOn = new Date(System.currentTimeMillis());
		save();
	}
	
	public void loggedOf()
	{
		firstTick = true;
		onlinePlayer = null;
		lastLoginOn = new Date(System.currentTimeMillis());
		save();
	}
	
	public boolean addFriend(Resident r)
	{
		for (Resident res : friends)
		{
			if (res == r)
				return false;
		}
		
		friends.add(r);
		save();
		return true;
	}
	
	public boolean removeFriend(Resident r)
	{
		if (friends.remove(r))
		{
			save();
			return true;
		}
		else
			return false;
	}
}