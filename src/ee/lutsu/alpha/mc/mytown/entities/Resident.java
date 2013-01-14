package ee.lutsu.alpha.mc.mytown.entities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Joiner;

import ee.lutsu.alpha.mc.mytown.ChatChannel;
import ee.lutsu.alpha.mc.mytown.ChunkCoord;
import ee.lutsu.alpha.mc.mytown.Formatter;
import ee.lutsu.alpha.mc.mytown.Log;
import ee.lutsu.alpha.mc.mytown.MyTown;
import ee.lutsu.alpha.mc.mytown.MyTownDatasource;
import ee.lutsu.alpha.mc.mytown.Permissions;
import ee.lutsu.alpha.mc.mytown.Term;
import ee.lutsu.alpha.mc.mytown.commands.CmdChat;
import ee.lutsu.alpha.mc.mytown.entities.TownSettingCollection.ISettingsSaveHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;

public class Resident 
{
	public static boolean ignoreOps = false;
	public static boolean allowMemberToMemberPvp = false;
	public static int pickupSpamCooldown = 5000;
	
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
	public Resident location2;
	public TownBlock checkYMovement = null;
	public boolean mapMode = false;
	public Town inviteActiveFrom;
	public ChatChannel activeChannel = ChatChannel.Global;
	public boolean beingBounced = false;
	public List<Resident> friends = new ArrayList<Resident>();
	
	public int prevDimension, prevDimension2;
	public double prevX, prevY, prevZ;
	public float prevYaw, prevPitch;
	public long pickupWarningCooldown = 0;
	
	public boolean firstTick = true;
	public boolean wasfirstTick = true;
	
	public boolean hasTown() {return town != null; }

	
	public void setActiveChannel(ChatChannel ch) { activeChannel = ch; save(); }
	public Town town(){ return town; }
	public void setTown(Town t)
	{ 
		town = t; 
		settings.setParent(t == null ? null : t.settings);
		if (t == null)
			settings.unlinkAllDown();
	}
	public Rank rank(){ return rank; }
	public void setRank(Rank r){ rank = r; }
	public String name() { return name; }
	public boolean isOnline() { return onlinePlayer != null && !onlinePlayer.isDead; }
	public int id() { return id; }
	public void setId(int val) { id = val; }
	public Date created() { return createdOn; }
	public Date lastLogin() { return lastLoginOn; }
	public TownSettingCollection settings = new TownSettingCollection();
	
	public Resident(String pName)
	{
		this();
		
		name = pName;
		createdOn = new Date(System.currentTimeMillis());
		lastLoginOn = new Date(System.currentTimeMillis());
		
		save();
	}
	
	protected Resident()
	{
		settings.tag = this;
		settings.saveHandler = new ISettingsSaveHandler() 
		{
			public void save(TownSettingCollection sender, Object tag) 
			{
				Resident r = (Resident)tag;
				r.save();
			}
		};
	}
	
	public boolean shouldShowTownBlocks()
	{
		return Permissions.canAccess(this, "mytown.adm.showblocks");
	}
	
	public boolean shouldShowPlayerLocation()
	{
		return Permissions.canAccess(this, "mytown.adm.showlocation");
	}
	
	public boolean canByPassCheck(TownSettingCollection.Permissions level)
	{
		return Permissions.canAccess(this, "mytown.adm.bypass." + level.toString().toLowerCase());
	}
	
	public boolean pvpBypass()
	{
		return Permissions.canAccess(this, "mytown.adm.bypass.pvp");
	}
	
	public boolean canInteract(int chunkX, int chunkZ, TownSettingCollection.Permissions askedFor)
	{
		TownBlock block = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, chunkX, chunkZ);

		return canInteract(block, askedFor);
	}
	
	public boolean canInteract(TownBlock block, TownSettingCollection.Permissions askedFor)
	{
		boolean b = canInteractSub(block, askedFor);
		if (!b && canByPassCheck(askedFor))
			b = true;
		
		return b;
	}
	
	private boolean canInteractSub(TownBlock block, TownSettingCollection.Permissions askedFor)
	{
		if (block == null || block.town() == null)
			return MyTown.instance.getWorldWildSettings(onlinePlayer.dimension).outsiderRights.compareTo(askedFor) >= 0;
		
		if (block.owner() == this || (block.town() == town() && rank() != Rank.Resident)) // plot owner
			return true;
		
		if (block.owner() != null && block.owner().friends.contains(this)) // friends
			return block.settings.friendRights.compareTo(askedFor) >= 0;
		
		if (town() == block.town()) // town member
			return block.settings.townMemberRights.compareTo(askedFor) >= 0;

		if (town() != null && town().nation() != null && town().nation() == block.town().nation()) // nation
			return block.settings.nationMemberRights.compareTo(askedFor) >= 0;
		
		return block.settings.outsiderRights.compareTo(askedFor) >= 0;
	}
	
	public boolean canInteract(TownBlock targetBlock, int y, TownSettingCollection.Permissions askedFor)
	{
		if (targetBlock == null || targetBlock.town() == null)
			return canInteract(null, askedFor);

		if (targetBlock.settings.yCheckOn)
		{
			if (y < targetBlock.settings.yCheckFrom || y > targetBlock.settings.yCheckTo)
			{
				targetBlock = targetBlock.getFirstFullSidingClockwise(targetBlock.town());
			}
		}
		
		return canInteract(targetBlock, askedFor);
	}
	
	public boolean canInteract(int dimension, int x, int y, int z, TownSettingCollection.Permissions askedFor)
	{
		TownBlock targetBlock = MyTownDatasource.instance.getBlock(dimension, ChunkCoord.getCoord(x), ChunkCoord.getCoord(z));
		if (targetBlock == null || targetBlock.town() == null)
			return MyTown.instance.getWorldWildSettings(dimension).outsiderRights.compareTo(askedFor) >= 0;
			
		return canInteract(targetBlock, y, askedFor);
	}
	
	public boolean canInteract(int x, int y, int z, TownSettingCollection.Permissions askedFor)
	{
		return canInteract(onlinePlayer.dimension, x, y, z, askedFor);
	}
	
	public boolean canInteract(Entity e)
	{
		TownBlock targetBlock = MyTownDatasource.instance.getBlock(e.dimension, e.chunkCoordX, e.chunkCoordZ);

		if (e instanceof EntityMinecart)
		{
			if ((targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction) || ((targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(e.dimension).allowCartInteraction))
				return true;
		}
		
		if (e instanceof EntityItem)
			return canInteract(targetBlock, (int)e.posY, TownSettingCollection.Permissions.Loot);
		else
			return canInteract(targetBlock, (int)e.posY, TownSettingCollection.Permissions.Build); // shears come here
	}
	
	public boolean canAttack(Entity e)
	{
		if (e instanceof EntityPlayer)
		{
			if (pvpBypass())
				return true;
			
			// disable friendly fire
			if (!allowMemberToMemberPvp && town() != null && MyTownDatasource.instance.getOrMakeResident((EntityPlayer)e).town() == town())
				return false;
			
			if (Town.allowFullPvp)
				return true;

			TownBlock targetBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, e.chunkCoordX, e.chunkCoordZ);
			if (targetBlock != null && targetBlock.town() != null)
			{
				if (targetBlock.settings.yCheckOn)
				{
					int y = (int)e.posY;
					if (y < targetBlock.settings.yCheckFrom || y > targetBlock.settings.yCheckTo)
						targetBlock = targetBlock.getFirstFullSidingClockwise(targetBlock.town());
				}
			
				if (targetBlock != null)
					return Town.allowMemberToForeignPvp && town() == targetBlock.town();
			}
			TownBlock sourceBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, onlinePlayer.chunkCoordX, onlinePlayer.chunkCoordZ);
			if (sourceBlock != null && sourceBlock.town() != null)
			{
				if (sourceBlock.settings.yCheckOn)
				{
					int y = (int)e.posY;
					if (y < sourceBlock.settings.yCheckFrom || y > sourceBlock.settings.yCheckTo)
						sourceBlock = sourceBlock.getFirstFullSidingClockwise(sourceBlock.town());
				}
			
				if (sourceBlock != null)
					return Town.allowMemberToForeignPvp && town() == sourceBlock.town();
			}
			
			return true;
		}
		else
		{
			TownBlock targetBlock = MyTownDatasource.instance.getBlock(onlinePlayer.dimension, e.chunkCoordX, e.chunkCoordZ);

			if (e instanceof EntityMinecart)
			{
				if ((targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction) || ((targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(e.dimension).allowCartInteraction))
					return true;
			}

			return canInteract(targetBlock, (int)e.posY, TownSettingCollection.Permissions.Build);
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
				
				boolean mid = z == cz && x == cx;
				boolean isTown = b != null && b.town() != null;
				boolean ownTown = isTown && b.town() == town;
				boolean takenPlot = ownTown && b.owner() != null;
				boolean ownPlot = takenPlot && b.owner() == this;
				
				if (mid)
					c = ownPlot ? "§e" : ownTown ? "§a" : isTown ? "§c" : "§f";
				else
					c = ownPlot ? "§6" : ownTown ? "§2" : isTown ? "§4" : "§7";
				
				c += takenPlot ? "X" : isTown ? "O" : "_";

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

		res.settings.setParent(town == null ? null : town.settings);
		res.settings.deserialize(extra);

		return res;
	}
	
	public String serializeExtra()
	{
		return settings.serialize();
	}

	public void checkLocation()
	{
		if (beingBounced)
			return;
		
		MyTownDatasource source = MyTownDatasource.instance;
		
		int pX = ChunkCoord.getCoord(onlinePlayer.posX);
		int pZ = ChunkCoord.getCoord(onlinePlayer.posZ);
		TownBlock block = checkYMovement;
		
		if (block == null)
			block = source.getBlock(onlinePlayer.dimension, pX, pZ);
		
		if (block == null && location != null)
		{
			// entered wild
			onlinePlayer.sendChatToPlayer(Term.PlayerEnteredWild.toString());
			location = null;
			location2 = null;
			checkYMovement = null;
		}
		else if (block != null && block.town() != null)
		{
			// entered town or another town
			if (!canInteract(block, (int)onlinePlayer.posY, TownSettingCollection.Permissions.Enter))
			{
				beingBounced = true;
				try
				{
					onlinePlayer.sendChatToPlayer(Term.TownYouCannotEnter.toString(block.town().name()));
					bounceBack();
					
					pX = ChunkCoord.getCoord(onlinePlayer.posX);
					pZ = ChunkCoord.getCoord(onlinePlayer.posZ);
					
					TownBlock block2 = source.getBlock(onlinePlayer.dimension, pX, pZ);
					if (block2 != null && block2.town() != null && !canInteract(block2, (int)onlinePlayer.posY, TownSettingCollection.Permissions.Enter))
					{
						// bounce failed, send to spawn
						Log.warning(String.format("Player %s is inside a enemy town %s (%s, %s, %s, %s) with bouncing on. Sending to spawn.",
								name(), block2.town().name(),
								onlinePlayer.dimension, onlinePlayer.posX, onlinePlayer.posY, onlinePlayer.posZ));
						
						sendToSpawn();
					}
				}
				finally
				{
					beingBounced = false;
				}
			}
			else
			{
				checkYMovement = block.settings.yCheckOn ? block : null;
				
				if (block.owner() != location2 || block.town() != location)
				{
					if (block.town() != location)
					{
						if (block.town() == town())
							onlinePlayer.sendChatToPlayer(Term.PlayerEnteredOwnTown.toString(block.town().name()));
						else
							onlinePlayer.sendChatToPlayer(Term.PlayerEnteredTown.toString(block.town().name()));
					}
					
					if (block.owner() == this)
						onlinePlayer.sendChatToPlayer(Term.PlayerEnteredOwnPlot.toString(block.owner().name()));
					else if (block.owner() != null)
						onlinePlayer.sendChatToPlayer(Term.PlayerEnteredOwnPlot.toString(block.owner().name()));
					else
						onlinePlayer.sendChatToPlayer(Term.PlayerEnteredUnclaimedPlot.toString());
					
					location = block.town();
					location2 = block.owner();
				}
			}
		}
	}
	
	public void bounceBack()
	{
		if (wasfirstTick)
			return;
		
		if (this.onlinePlayer instanceof EntityPlayerMP)
		{
			if (this.onlinePlayer.dimension != this.prevDimension)
				MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension((EntityPlayerMP)this.onlinePlayer, this.prevDimension);

			if (onlinePlayer.ridingEntity != null)
				onlinePlayer.ridingEntity.moveEntity(this.prevX, this.prevY, this.prevZ);
			
			((EntityPlayerMP)this.onlinePlayer).playerNetServerHandler.setPlayerLocation(this.prevX, this.prevY, this.prevZ, this.prevYaw, this.prevPitch);
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
		
		ChunkCoordinates c = onlinePlayer.getBedLocation();
		
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
	
	public void sendToTownSpawn(Town t)
	{
		if (!(onlinePlayer instanceof EntityPlayerMP))
			throw new RuntimeException("Cannot move a non-player");
		
		if (t.spawnBlock == null || t.getSpawn() == null)
			throw new RuntimeException("Town doesn't have a spawn");
		
		EntityPlayerMP pl = (EntityPlayerMP)onlinePlayer;
		
		if (pl.dimension != t.getSpawnDimension())
			MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(pl, t.getSpawnDimension());

		Vec3 pos = t.getSpawn();
		pl.playerNetServerHandler.setPlayerLocation(pos.xCoord, pos.yCoord, pos.zCoord, t.getSpawnEye2(), t.getSpawnEye1());
	}
	
	public void sendToServerSpawn()
	{
		if (!(onlinePlayer instanceof EntityPlayerMP))
			throw new RuntimeException("Cannot move a non-player");

		EntityPlayerMP pl = (EntityPlayerMP)onlinePlayer;
		
		if (pl.dimension != 0)
			MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(pl, 0);

		WorldInfo info = pl.worldObj.getWorldInfo();
		int y = pl.worldObj.getHeightValue(info.getSpawnX(), info.getSpawnZ());
		
		if (info.getSpawnY() > 0 && info.getSpawnY() != pl.worldObj.provider.getAverageGroundLevel())
			y = info.getSpawnY();

		pl.setPositionAndUpdate(info.getSpawnX(), y, info.getSpawnZ());
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
		
		if (teleportToSpawnStamp != 0)
		{
			if (teleportToSpawnStamp <= System.currentTimeMillis())
				asyncEndSpawnTeleport();
			else if ((int)onlinePlayer.posX != (int)prevX || (int)onlinePlayer.posZ != (int)prevZ || (int)onlinePlayer.posY != (int)prevY)
				asyncResetSpawnTeleport();
		}
			
					
		wasfirstTick = false;
		
		if (firstTick)
		{
			firstTick = false;
			wasfirstTick = true;
		}
		else
		{
			int cX = ChunkCoord.getCoord(onlinePlayer.posX);
			int cZ = ChunkCoord.getCoord(onlinePlayer.posZ);
			int pcX = ChunkCoord.getCoord(prevX);
			int pcZ = ChunkCoord.getCoord(prevZ);

			if (prevDimension != onlinePlayer.dimension || cX != pcX || cZ != pcZ)
			{
				checkYMovement = null;
				checkLocation();
				
				if (mapMode)
					sendLocationMap(onlinePlayer.dimension, cX, cZ);
			}
			else if (checkYMovement != null && onlinePlayer.posY != prevY)
				checkLocation();

			prevDimension = onlinePlayer.dimension;
			prevX = onlinePlayer.posX;
			prevY = onlinePlayer.posY;
			prevZ = onlinePlayer.posZ;
			prevYaw = onlinePlayer.rotationYaw;
			prevPitch = onlinePlayer.rotationPitch;
		}
	}
	
	public String formattedName()
	{
		return prefix() + name() + postfix();
	}
	
	public void sendInfoTo(ICommandSender cs, boolean adminInfo)
	{
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		List<String> fnames = new ArrayList<String>();
		List<String> fnames2 = new ArrayList<String>();
		
		for (Resident r : friends)
			fnames.add(Formatter.formatResidentName(r));
		
		for (Resident r : MyTownDatasource.instance.residents)
		{
			if (r.friends.contains(this))
				fnames2.add(Formatter.formatResidentName(r));
		}
		
		String sFriends = Joiner.on("§2, ").join(fnames);
		String sFriends2 = Joiner.on("§2, ").join(fnames2);
		
		cs.sendChatToPlayer(Term.ResStatusName.toString(Formatter.formatResidentName(this)));
		
		if (adminInfo && isOnline())
			cs.sendChatToPlayer(Term.ResStatusLocation.toString(location != null ? location.name() : "wild", onlinePlayer.dimension, (int)onlinePlayer.posX, (int)onlinePlayer.posY, (int)onlinePlayer.posZ));
		
		cs.sendChatToPlayer(Term.ResStatusGeneral1.toString(format.format(createdOn))); 
		cs.sendChatToPlayer(Term.ResStatusGeneral2.toString(isOnline() ? "online" : format.format(lastLoginOn)));
		cs.sendChatToPlayer(Term.ResStatusTown.toString(
			town == null ? "none" : town().name(),
			town == null ? "Loner" : rank.toString()));
		
		if (fnames.size() > 0)
			cs.sendChatToPlayer(Term.ResStatusFriends.toString(sFriends));
		if (fnames2.size() > 0)
			cs.sendChatToPlayer(Term.ResStatusFriends2.toString(sFriends2));
		
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
	
	private long teleportToSpawnStamp = 0;
	public static long teleportToSpawnWait = 1 * 60 * 1000; // 1 minute
	public void asyncStartSpawnTeleport()
	{
		long takesTime = Permissions.canAccess(this, "mytown.adm.bypass.teleportwait") ? 0 : teleportToSpawnWait;
		teleportToSpawnStamp = System.currentTimeMillis() + takesTime;
		
		if (takesTime > 0)
		{
			CmdChat.sendChatToAround(onlinePlayer.dimension, onlinePlayer.posX, onlinePlayer.posY, onlinePlayer.posZ, Term.SpawnCmdTeleportNearStarted.toString(name(), (int)Math.ceil(takesTime / 1000)), onlinePlayer);
			onlinePlayer.sendChatToPlayer(Term.SpawnCmdTeleportStarted.toString((int)Math.ceil(takesTime / 1000)));
		}
	}
	
	private void asyncResetSpawnTeleport() // when the player moved
	{
		if (!isOnline())
			return;
		
		teleportToSpawnStamp = 0;
		onlinePlayer.sendChatToPlayer(Term.SpawnCmdTeleportReset.toString());
	}
	
	private void asyncEndSpawnTeleport() // time out, move it
	{
		if (!isOnline())
			return;
		
		teleportToSpawnStamp = 0;

		sendToServerSpawn();
		
		onlinePlayer.sendChatToPlayer(Term.SpawnCmdTeleportEnded.toString());
	}
}
