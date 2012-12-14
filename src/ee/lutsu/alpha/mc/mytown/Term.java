package ee.lutsu.alpha.mc.mytown;

import java.util.HashMap;

public enum Term
{
	// General & events
	ChatFormat("§f[$color$$channel$§f]$prefix$$name$$postfix$$color$: $msg$"),
	ChatErrNotInTown("You cannot use townchat if you are not in a town"),
	ChatAloneInChannel("§7§oSo lonely..."),
	ChatListStart("Switch chat channel"),
	ChatListEntry("   %s%s §f[%s%s§f]"),
	ChatSwitch("Changed active channel to [%s%s§f] %s%s"),
	
	SevereLevel("SEVERE"),
	WarningLevel("WARNING"),
	InfoLevel("INFO"),
	
	MinecartMessedWith("Someone is messing with your towns minecart"),
	
	// status
	TownStatusName("§6--------[[ %s%s§6 ]]--------"),
	TownStatusGeneral("§2Town blocks: §b%s§2/§b%s §2Bounce?: %s"),
	TownStatusMayor("§2Mayor: §b%s"),
	TownStatusAssistants("§2Assistants: §b%s"),
	TownStatusResidents("§2Residents: §b%s"),
	
	// resident status
	ResStatusName("§6--------[[ §2%s§6 ]]--------"),
	ResStatusGeneral1("§2Member from: §b%s"),
	ResStatusGeneral2("§2Last online: §b%s"),
	ResStatusTown("§2Member of: §b%s§2, §a%s"),
	
	// commands
	TownCommand("mytown"),
	TownCommandAliases("t town"),
	
	TownAdmCommand("mytownadm"),
	TownAdmCommandAliases("ta"),
	
	ChannelCommand("ch"),
	ChannelCommandAliases(""),
	
	TownCommandDesc("§5MyTown §2- Be protected by towns"),
	LineSeperator("§6--------[[ §5MyTown§6 ]]--------"),
	CommandHelp("help"),
	
	// all
	TownCmdMap("map"),
	TownCmdMapArgs("[on|off]"),
	TownCmdMapDesc("Shows map or toggles map mode on/off"),
	
	TownCmdInfo("info"),
	TownCmdInfoArgs("townname"),
	TownCmdInfoDesc("Shows info about the town"),
	
	TownCmdRes("res"),
	TownCmdResArgs("playername"),
	TownCmdResDesc("Shows info about the resident"),
	
	TownCmdList("list"),
	TownCmdListDesc("Lists all towns"),
	TownCmdListStart("§aTowns§f: %s"),
	TownCmdListEntry("%s[%s]"),
	
	// mayor commands
	TownCmdAssistant("assistant"),
	TownCmdAssistantArgs("add|remove name"),
	TownCmdAssistantArgs1("add"),
	TownCmdAssistantArgs2("remove"),
	TownCmdAssistantDesc("Allows people to manage residents and land"),
	
	TownCmdMayor("mayor"),
	TownCmdMayorArgs("[name]"),
	TownCmdMayorDesc("Sets a new mayor"),
	
	TownCmdRename("rename"),
	TownCmdRenameArgs("[name]"),
	TownCmdRenameDesc("Sets a new town name"),
	
	TownCmdBounce("bounce"),
	TownCmdBounceDesc("Toggle non-member bounce mode"),
	
	TownCmdDelete("delete"),
	TownCmdDeleteDesc("Deletes your town"),
	TownCmdDeleteAction("Are you sure? Use /t delete ok"),
	
	// assistant commands
	TownCmdClaim("claim"),
	TownCmdClaimArgs("[rect X]"),
	TownCmdClaimArgs1("rect"),
	TownCmdClaimDesc("Claim land for your town"),
	
	TownCmdUnclaim("unclaim"),
	TownCmdUnclaimArgs("[rect X]"),
	TownCmdUnclaimArgs1("rect"),
	TownCmdUnclaimDesc("Unclaim land"),
	
	TownCmdInvite("invite"),
	TownCmdInviteArgs("name"),
	TownCmdInviteDesc("Invite a player to join your town"),
	
	TownCmdKick("kick"),
	TownCmdKickArgs("name"),
	TownCmdKickDesc("Remove the player from your town"),
	
	// resident commands
	TownCmdLeave("leave"),
	TownCmdLeaveDesc("Leave the town"),
	
	TownCmdOnline("online"),
	TownCmdOnlineDesc("Shows the players online in your town"),
	
	// non-residents
	TownCmdNew("new"),
	TownCmdNewArgs("name"),
	TownCmdNewDesc("Creates a new town"),
	
	TownCmdAccept("accept"),
	TownCmdAcceptDesc("Accept a town invitation"),
	
	TownCmdDeny("deny"),
	TownCmdDenyDesc("Deny a town invitation"),
	
	// admin commands
	TownadmCmdReload("reload"),
	TownadmCmdReloadDesc("Reload the config, db and terms"),
	
	TownadmCmdNew("new"),
	TownadmCmdNewArgs("townname mayorname"),
	TownadmCmdNewDesc("Creates a new town"),
	
	TownadmCmdDelete("delete"),
	TownadmCmdDeleteArgs("townname"),
	TownadmCmdDeleteDesc("Deletes a town"),
	
	TownadmCmdSet("set"),
	TownadmCmdSetArgs("townname rank name, name .."),
	TownadmCmdSetDesc("Adds members to a town or sets ranks"),
	
	TownadmCmdRem("rem"),
	TownadmCmdRemArgs("townname name, name .."),
	TownadmCmdRemDesc("Removes members from a town"),
	
	TownadmCmdExtra("extra"),
	TownadmCmdExtraArgs("townname count"),
	TownadmCmdExtraDesc("Adds or removes extra blocks in a town"),

	// Town errors
	ErrCannotAccessCommand("You cannot access this command"),
	TownErrAlreadyClaimed("This block is claimed by another town"),
	TownErrPlayerAlreadyInTown("That player is already part of a town"),
	TownErrPlayerNotFound("Player not found"),
	TownErrPlayerNotFoundOrOnline("Player not found or not online"),
	TownErrBlockTooCloseToAnotherTown("This block is too close to another town"),
	TownErrCreatorPartOfTown("You cannot be part of a town"),
	TownErrTownNameCannotBeEmpty("Cannot set a empty name"),
	TownErrTownNameAlreadyInUse("This town name has already need used"),
	TownErrNoFreeBlocks("You don't have any free blocks"),
	
	TownErrCmdUnknownArgument("Unknown argument: §4%s"),
	TownErrCmdNumberFormatException("The input isn't numerical"),
	
	TownErrInvitationSelf("The fuck are you doing? Invite OTHERS"),
	TownErrInvitationAlreadyInYourTown("Hes in your town moron"),
	TownErrInvitationActive("The player has a pending invitation already"),
	TownErrInvitationInTown("The player is already in a town"),
	
	TownErrPlayerNotInYourTown("The player is not in your town"),
	TownErrCannotDoWithYourself("You cannot do this with yourself"),
	TownErrPlayerIsAlreadyMayor("The player is already a mayor"),
	TownErrPlayerIsAlreadyAssistant("The player is already an assistant"),
	TownErrPlayerIsNotAssistant("The player isn't an assistant"),
	TownErrCannotUseThisDemoteMayor("Cannot use this to demote a mayor"),
	
	TownErrCannotKickAssistants("You cannot kick another assistant"),
	TownErrCannotKickMayor("You cannot kick mayors"),
	TownErrMayorsCantLeaveTheTown("You cannot leave the town as a mayor"),
	TownErrCannotKickYourself("You cannot kick yourself"),
	
	TownErrYouDontHavePendingInvitations("You don't have any pending invitation"),
	TownErrNotFound("Town named %s cannot be found"),
	
	// Globals
	TownBroadcastCreated("%s has just founded a new town called %s"),
	TownBroadcastDeleted("The town of %s went like POOF"),
	
	TownBroadcastLoggedIn("%s came just online"),
	TownBroadcastLoggedOut("%s went just offline"),
	
	TownadmCreatedNewTown("Town named %s created for mayor %s"),
	TownadmDeletedTown("Town named %s deleted"),
	
	TownadmResidentsSet("Town residents set"),
	TownadmExtraSet("Town extra blocks value set"),
	
	PlayerEnteredWild("§aYou just entered the §2wilderness"),
	PlayerEnteredTown("§aWelcome to §4%s"),
	PlayerEnteredOwnTown("§aWelcome back to §2%s"),
	
	PlayerMapModeOn("§aTown map mode is now §2ON"),
	PlayerMapModeOff("§aTown map mode is now §4OFF"),
	
	TownBlocksClaimed("§a%s blocks claimed [%s]"),
	TownBlocksUnclaimed("§a%s blocks unclaimed [%s]"),
	TownMapHead("§6--------[[ §5MyTown§6 ]]--------"),
	
	TownInvitedPlayer("§aSent the town invitation to %s"),
	TownKickedPlayer("%s kicked the player %s from town"),
	TownPlayerLeft("%s left the town"),
	TownInvitation("§a%s would like you to join his town %s. Use /t §2accept §aor /t §4deny §ato reply"),
	TownRenamed("Your town charter has been renamed to %s"),
	
	TownPlayerPromotedToAssistant("Player %s promoted to be an assistant"),
	TownPlayerDemotedFromAssistant("Player %s demoted to a normal resident"),
	TownPlayerPromotedToMayor("Player %s promoted to be town mayor"),
	
	
	TownPlayerJoinedTown("The player %s has joined the town"),
	TownPlayerDeniedInvitation("You have denied the invitation"),
	
	TownadmModReloaded("The mod has been reloaded"),
	
	TownPlayersOnlineStart("§aPlayers online: %s"),
	TownYouCannotEnter("§aYou cannot enter the town §4%s§a. Town rules."),
	
	TownBouncing("§4Bouncing"),
	TownNotBouncing("§2Not bouncing"),
	TownBouncingChanged("§aThe town is now in %s §astatus"),
	ChatTownLogFormat("§f[§a%s§f]%s"),
	;
	
	public final String defaultVal;
	public static String language = null;
	public static HashMap<String, HashMap<Term, String>> translations = new HashMap<String, HashMap<Term, String>>();
	
	Term(String def)
	{
		defaultVal = def;
	}
	
	public String fname()
	{
		return super.toString();
	}
	
	@Override
	public String toString()
	{
		if (language == null)
			return defaultVal;
		
		HashMap<Term, String> terms = translations.get(language);
		if (terms == null)
			return defaultVal;
		
		String s = terms.get(this);
		
		return s == null || s.equals("") ? defaultVal : s;
	}
	
	public String toString(Object ... params)
	{
		return String.format(toString(), params);
	}
	
	public static void translate(String lang, Term term, String val)
	{
		HashMap<Term, String> terms = translations.get(lang);
		if (terms == null)
			translations.put(lang, terms = new HashMap<Term, String>());
		
		terms.put(term, val);
	}
}