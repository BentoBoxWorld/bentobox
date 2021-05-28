package world.bentobox.bentobox.database.objects;

import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.metadata.MetaDataAble;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.adapters.Adapter;
import world.bentobox.bentobox.database.objects.adapters.FlagSerializer;
import world.bentobox.bentobox.database.objects.adapters.FlagSerializer3;
import world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Stores all the info about an island
 * Managed by IslandsManager
 * Responsible for team information as well.
 *
 * @author tastybento
 * @author Poslovitch
 */
@Table(name = "Islands")
public class Island implements DataObject, MetaDataAble {

    /**
     * Set to true if this data object has been changed since being loaded from the database
     */
    private boolean changed;

    // True if this island is deleted and pending deletion from the database
    @Expose
    private boolean deleted = false;

    @Expose
    @NonNull
    private String uniqueId = UUID.randomUUID().toString();

    //// Island ////
    // The center of the island space
    @Expose
    @Nullable
    private Location center;

    /**
     * The center location of the protection area
     */
    @Expose
    @Nullable
    private Location location;


    // Island range
    @Expose
    private int range;

    // Protection size
    @Expose
    private int protectionRange;

    // Maximum ever protection range - used in island deletion
    @Expose
    private int maxEverProtectionRange;

    // World the island started in. This may be different from the island location
    @Expose
    private World world;

    /**
     * Name of the {@link world.bentobox.bentobox.api.addons.GameModeAddon GameModeAddon} this island is handled by.
     * @since 1.5.0
     */
    @Expose
    private String gameMode;

    // Display name
    @Expose
    @Nullable
    private String name;

    // Time parameters
    @Expose
    private long createdDate;
    @Expose
    private long updatedDate;

    //// Team ////
    /**
     * Owner of the island.
     * There can only be one per island.
     * If it is {@code null}, then the island is considered as unowned.
     */
    @Expose
    @Nullable
    private UUID owner;

    /**
     * Members of the island.
     * It contains any player which has one of the following rank on this island: {@link RanksManager#COOP_RANK COOP},
     * {@link RanksManager#TRUSTED_RANK TRUSTED}, {@link RanksManager#MEMBER_RANK MEMBER}, {@link RanksManager#SUB_OWNER_RANK SUB_OWNER},
     * {@link RanksManager#OWNER_RANK OWNER}.
     */
    @Expose
    private Map<UUID, Integer> members = new HashMap<>();

    /**
     * Maximum number of members allowed in this island.
     * Key is rank, value is number
     * @since 1.16.0
     */
    @Expose
    private Map<Integer, Integer> maxMembers;

    //// State ////
    @Expose
    private boolean spawn = false;
    @Expose
    private boolean purgeProtected = false;

    //// Protection flags ////
    @Adapter(FlagSerializer.class)
    @Expose
    private Map<Flag, Integer> flags = new HashMap<>();

    //// Island History ////
    @Adapter(LogEntryListAdapter.class)
    @Expose
    private List<LogEntry> history = new LinkedList<>();

    @Expose
    private int levelHandicap;
    @Expose
    private Map<Environment, Location> spawnPoint = new EnumMap<>(Environment.class);

    /**
     * This flag is used to quarantine islands that cannot be loaded and should be purged at some point
     */
    @Expose
    private boolean doNotLoad;

    /**
     * Used to store flag cooldowns for this island
     */
    @Adapter(FlagSerializer3.class)
    @Expose
    private Map<Flag, Long> cooldowns = new HashMap<>();

    /**
     * Commands and the rank required to use them for this island
     */
    @Expose
    private Map<String, Integer> commandRanks;

    /**
     * If true then this space is reserved for the owner and when they teleport there they will be asked to make an island
     * @since 1.6.0
     */
    @Expose
    @Nullable
    private Boolean reserved = null;

    /**
     * A place to store meta data for this island.
     * @since 1.15.4
     */
    @Expose
    private Map<String, MetaDataValue> metaData;

    /**
     * Island homes. Replaces player homes
     * @since 1.16.0
     */
    @Expose
    private Map<String, Location> homes;

    /**
     * The maximum number of homes allowed on this island. If null, then the world default is used.
     */
    @Expose
    private Integer maxHomes;

    /*
     * *************************** Constructors ******************************
     */

    public Island() {}

    public Island(@NonNull Location location, UUID owner, int protectionRange) {
        setOwner(owner);
        createdDate = System.currentTimeMillis();
        updatedDate = System.currentTimeMillis();
        world = location.getWorld();
        // Make a copy of the location
        center = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
        range = BentoBox.getInstance().getIWM().getIslandDistance(world);
        this.protectionRange = protectionRange;
        this.maxEverProtectionRange = protectionRange;
        this.setChanged();
    }


    /**
     * Clones an island object
     * @param island - island to clone
     */
    public Island(Island island) {
        this.center = island.getCenter().clone();
        this.createdDate = island.getCreatedDate();
        Optional.ofNullable(island.getCommandRanks()).ifPresent(cr -> {
            this.commandRanks = new HashMap<>();
            this.commandRanks.putAll(cr);
        });
        Optional.ofNullable(island.getCooldowns()).ifPresent(c -> {
            this.cooldowns = new HashMap<>();
            this.cooldowns.putAll(c);
        });
        this.createdDate = island.getCreatedDate();
        this.deleted = island.isDeleted();
        this.doNotLoad = island.isDoNotLoad();
        this.flags.putAll(island.getFlags());
        this.gameMode = island.getGameMode();
        this.homes = new HashMap<>(island.getHomes());
        this.history.addAll(island.getHistory());
        this.levelHandicap = island.getLevelHandicap();
        this.location = island.getProtectionCenter();
        this.maxEverProtectionRange = island.getMaxEverProtectionRange();
        this.maxHomes = island.getMaxHomes();
        this.maxMembers = new HashMap<>(island.getMaxMembers());
        this.members.putAll(island.getMembers());
        island.getMetaData().ifPresent(m -> {
            this.metaData = new HashMap<>();
            this.metaData.putAll(m);
        });
        this.name = island.getName();
        this.owner = island.getOwner();
        this.protectionRange = island.getProtectionRange();
        this.purgeProtected = island.getPurgeProtected();
        this.range = island.getRange();
        this.reserved = island.isReserved();
        this.spawn = island.isSpawn();
        island.getSpawnPoint().forEach((k,v) -> island.spawnPoint.put(k, v.clone()));
        this.uniqueId = island.getUniqueId();
        this.updatedDate = island.getUpdatedDate();
        this.world = island.getWorld();
        this.setChanged();
    }

    /*
     * *************************** Methods ******************************
     */

    /**
     * Adds a team member. If player is on banned list, they will be removed from it.
     * @param playerUUID - the player's UUID
     */
    public void addMember(@NonNull UUID playerUUID) {
        setRank(playerUUID, RanksManager.MEMBER_RANK);
        setChanged();
    }

    /**
     * Bans the target player from this Island.
     * If the player is a member, coop or trustee, they will be removed from those lists.
     * <br/>
     * Calling this method won't call the {@link world.bentobox.bentobox.api.events.island.IslandEvent.IslandBanEvent}.
     * @param issuer UUID of the issuer, may be null.
     *               Whenever possible, one should be provided.
     * @param target UUID of the target, must be provided.
     * @return {@code true}
     */
    public boolean ban(@NonNull UUID issuer, @NonNull UUID target) {
        setRank(target, RanksManager.BANNED_RANK);
        log(new LogEntry.Builder("BAN").data("player", target.toString()).data("issuer", issuer.toString()).build());
        setChanged();
        return true;
    }

    /**
     * @return the banned
     */
    public Set<UUID> getBanned() {
        Set<UUID> result = new HashSet<>();
        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() <= RanksManager.BANNED_RANK) {
                result.add(member.getKey());
            }
        }
        return result;
    }

    /**
     * Unbans the target player from this Island.
     * <br/>
     * Calling this method won't call the {@link world.bentobox.bentobox.api.events.island.IslandUnbanEvent}.
     * @param issuer UUID of the issuer, may be null.
     *               Whenever possible, one should be provided.
     * @param target UUID of the target, must be provided.
     * @return {@code true} if the target is successfully unbanned, {@code false} otherwise.
     */
    public boolean unban(@NonNull UUID issuer, @NonNull UUID target) {
        if (members.remove(target) != null) {
            log(new LogEntry.Builder("UNBAN").data("player", target.toString()).data("issuer", issuer.toString()).build());
            return true;
        }
        return false;
    }

    /**
     * Returns a clone of the location of the center of this island.
     * @return clone of the center Location
     */
    @Nullable
    public Location getCenter(){
        return center == null ? null : center.clone();
    }

    /**
     * @return the date when the island was created
     */
    public long getCreatedDate(){
        return createdDate;
    }

    /**
     * Gets the Island Guard flag's setting. If this is a protection flag, the this will be the
     * rank needed to bypass this flag. If it is a Settings flag, any non-zero value means the
     * setting is allowed.
     * @param flag - flag
     * @return flag value
     */
    public int getFlag(@NonNull Flag flag) {
        return flags.computeIfAbsent(flag, k -> flag.getDefaultRank());
    }

    /**
     * @return the flags
     */
    public Map<Flag, Integer> getFlags() {
        return flags;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * Returns the members of this island.
     * It contains all players that have any rank on this island, including {@link RanksManager#BANNED_RANK BANNED},
     * {@link RanksManager#TRUSTED_RANK TRUSTED}, {@link RanksManager#MEMBER_RANK MEMBER}, {@link RanksManager#SUB_OWNER_RANK SUB_OWNER},
     * {@link RanksManager#OWNER_RANK OWNER}, etc.
     *
     * @return the members - key is the UUID, value is the RanksManager enum, e.g. {@link RanksManager#MEMBER_RANK}.
     * @see #getMemberSet()
     */
    public Map<UUID, Integer> getMembers() {
        return members;
    }

    /**
     * Returns an immutable set containing the UUIDs of players that are truly members of this island.
     * This includes any player which has one of the following rank on this island: {@link RanksManager#MEMBER_RANK MEMBER},
     * {@link RanksManager#SUB_OWNER_RANK SUB_OWNER}, {@link RanksManager#OWNER_RANK OWNER}.
     * @return the members of the island (owner included)
     * @see #getMembers()
     */
    public ImmutableSet<UUID> getMemberSet(){
        return getMemberSet(RanksManager.MEMBER_RANK);
    }

    /**
     * Returns an immutable set containing the UUIDs of players with rank above that requested rank inclusive
     * @param minimumRank minimum rank (inclusive) of members
     * @return immutable set of UUIDs
     * @see #getMembers()
     * @since 1.5.0
     */
    public @NonNull ImmutableSet<UUID> getMemberSet(int minimumRank) {
        Builder<UUID> result = new ImmutableSet.Builder<>();
        members.entrySet().stream().filter(e -> e.getValue() >= minimumRank).map(Map.Entry::getKey).forEach(result::add);
        return result.build();
    }

    /**
     * Returns an immutable set containing the UUIDs of players with rank equal or above that requested rank (inclusive).
     * @param rank rank to request
     * @param includeAboveRanks whether including players with rank above the requested rank or not
     * @return immutable set of UUIDs
     * @see #getMemberSet(int)
     * @see #getMembers()
     * @since 1.5.0
     */
    public @NonNull ImmutableSet<UUID> getMemberSet(int rank, boolean includeAboveRanks) {
        if (includeAboveRanks) {
            return getMemberSet(rank);
        }
        Builder<UUID> result = new ImmutableSet.Builder<>();
        members.entrySet().stream().filter(e -> e.getValue() == rank).map(Map.Entry::getKey).forEach(result::add);
        return result.build();
    }

    /**
     * Get the minimum protected X block coord based on the island location.
     * It will never be less than {@link #getMinX()}
     * @return the minProtectedX
     */
    public int getMinProtectedX() {
        return getProtectionCenter() == null ? 0 : Math.max(getMinX(), getProtectionCenter().getBlockX() - protectionRange);
    }

    /**
     * Get the maximum protected X block coord based on the island location.
     * It will never be more than {@link #getMaxX()}
     * @return the maxProtectedX
     * @since 1.5.2
     */
    public int getMaxProtectedX() {
        return getProtectionCenter() == null ? 0 : Math.min(getMaxX(), getProtectionCenter().getBlockX() + protectionRange);
    }

    /**
     * Get the minimum protected Z block coord based on the island location.
     * It will never be less than {@link #getMinZ()}
     * @return the minProtectedZ
     */
    public int getMinProtectedZ() {
        return getProtectionCenter() == null ? 0 : Math.max(getMinZ(), getProtectionCenter().getBlockZ() - protectionRange);
    }

    /**
     * Get the maximum protected Z block coord based on the island location.
     * It will never be more than {@link #getMinZ()}
     * @return the maxProtectedZ
     * @since 1.5.2
     */
    public int getMaxProtectedZ() {
        return getProtectionCenter() == null ? 0 : Math.min(getMaxZ(), getProtectionCenter().getBlockZ() + protectionRange);
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return center.getBlockX() - range;
    }

    /**
     * @return the maxX
     * @since 1.5.2
     */
    public int getMaxX() {
        return center.getBlockX() + range;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return center.getBlockZ() - range;
    }

    /**
     * @return the maxZ
     * @since 1.5.2
     */
    public int getMaxZ() {
        return center.getBlockZ() + range;
    }

    /**
     * @return the island display name. Might be {@code null} if none is set.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Returns the owner of this island.
     * @return the owner, may be null.
     * @see #isOwned()
     * @see #isUnowned()
     */
    @Nullable
    public UUID getOwner(){
        return owner;
    }

    /**
     * Returns whether this island is owned or not.
     * @return {@code true} if this island has an owner, {@code false} otherwise.
     * @since 1.9.1
     * @see #getOwner()
     * @see #isUnowned()
     */
    public boolean isOwned() {
        return owner != null;
    }

    /**
     * Returns whether this island does not have an owner.
     * @return {@code true} if this island does not have an owner, {@code false} otherwise.
     * @since 1.9.1
     * @see #getOwner()
     * @see #isOwned()
     */
    public boolean isUnowned() {
        return owner == null;
    }

    /**
     * Returns the protection range of this Island.
     * This represents half of the length of the side of a theoretical square around the island center inside which flags are enforced.
     * @return the protection range of this island, strictly positive integer.
     * @see #getRange()
     */
    public int getProtectionRange() {
        return protectionRange;
    }

    /**
     * @return the maxEverProtectionRange or the protection range, whichever is larger
     */
    public int getMaxEverProtectionRange() {
        return Math.max(protectionRange, maxEverProtectionRange);
    }

    /**
     * @param maxEverProtectionRange the maxEverProtectionRange to set
     */
    public void setMaxEverProtectionRange(int maxEverProtectionRange) {
        this.maxEverProtectionRange = maxEverProtectionRange;
        setChanged();
    }

    /**
     * @return true if the island is protected from the Purge, otherwise false
     */
    public boolean getPurgeProtected(){
        return purgeProtected;
    }

    /**
     * Returns the island range.
     * It is a convenience method that returns the exact same value than island range, although it has been saved into the Island object for easier access.
     * @return the island range
     * @see #getProtectionRange()
     */
    public int getRange(){
        return range;
    }

    /**
     * Get the rank of user for this island
     * @param user - the User
     * @return rank integer
     */
    public int getRank(User user) {
        return members.getOrDefault(user.getUniqueId(), RanksManager.VISITOR_RANK);
    }

    /**
     * Get the rank of user for this island
     * @param userUUID - the User's UUID
     * @return rank integer
     * @since 1.14.0
     */
    public int getRank(UUID userUUID) {
        return members.getOrDefault(userUUID, RanksManager.VISITOR_RANK);
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return the date when the island was updated (team member connection, etc...)
     */
    public long getUpdatedDate(){
        return updatedDate;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the x coordinate of the island center
     */
    public int getX(){
        return center.getBlockX();
    }

    /**
     * @return the y coordinate of the island center
     */
    public int getY(){
        return center.getBlockY();
    }

    /**
     * @return the z coordinate of the island center
     */
    public int getZ(){
        return center.getBlockZ();
    }

    /**
     * Checks if coords are in the island space
     * @param x - x coordinate
     * @param z - z coordinate
     * @return true if in the island space
     */
    public boolean inIslandSpace(int x, int z) {
        return x >= getMinX() && x < getMinX() + range*2 && z >= getMinZ() && z < getMinZ() + range*2;
    }

    /**
     * Checks if location is in full island space, not just protected space
     * @param location - location
     * @return true if in island space
     */
    public boolean inIslandSpace(Location location) {
        return Util.sameWorld(world, location.getWorld()) && inIslandSpace(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Checks if the coordinates are in full island space, not just protected space
     * @param blockCoordinates - Pair(x,z) coordinates of block
     * @return true or false
     */
    public boolean inIslandSpace(Pair<Integer, Integer> blockCoordinates) {
        return inIslandSpace(blockCoordinates.x, blockCoordinates.z);
    }

    /**
     * Returns a {@link BoundingBox} of the full island space.
     * @return a {@link BoundingBox} of the full island space.
     * @since 1.5.2
     */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(getMinX(), 0.0D, getMinZ(), getMaxX()-1.0D, world.getMaxHeight(), getMaxZ()-1.0D);
    }

    /**
     * Returns a list of players that are physically inside the island's protection range and that are visitors.
     * @return list of visitors
     * @since 1.3.0
     */
    @NonNull
    public List<Player> getVisitors() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> onIsland(player.getLocation()) && getRank(User.getInstance(player)) == RanksManager.VISITOR_RANK)
                .collect(Collectors.toList());
    }

    /**
     * Returns whether this Island has visitors inside its protection range.
     * Note this is equivalent to {@code !island.getVisitors().isEmpty()}.
     * @return {@code true} if there are visitors inside this Island's protection range, {@code false} otherwise.
     *
     * @since 1.3.0
     * @see #getVisitors()
     */
    public boolean hasVisitors() {
        return Bukkit.getOnlinePlayers().stream().anyMatch(player -> onIsland(player.getLocation()) && getRank(User.getInstance(player)) == RanksManager.VISITOR_RANK);
    }

    /**
     * Returns a list of players that are physically inside the island's protection range
     * @return list of players
     * @since 1.6.0
     */
    @NonNull
    public List<Player> getPlayersOnIsland() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> onIsland(player.getLocation()))
                .collect(Collectors.toList());
    }

    /**
     * Returns whether this Island has players inside its protection range.
     * Note this is equivalent to {@code !island.getPlayersOnIsland().isEmpty()}.
     * @return {@code true} if there are players inside this Island's protection range, {@code false} otherwise.
     *
     * @since 1.6.0
     * @see #getPlayersOnIsland()
     */
    public boolean hasPlayersOnIsland() {
        return Bukkit.getOnlinePlayers().stream().anyMatch(player -> onIsland(player.getLocation()));
    }

    /**
     * Check if the flag is allowed or not
     * For flags that are for the island in general and not related to rank.
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(Flag flag) {
        // A negative value means not allowed
        return getFlag(flag) >= 0;
    }

    /**
     * Check if a user is allowed to bypass the flag or not
     * @param user - the User - user
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(User user, Flag flag) {
        return getRank(user) >= getFlag(flag);
    }

    /**
     * Check if banned
     * @param targetUUID - the target's UUID
     * @return Returns true if target is banned on this island
     */
    public boolean isBanned(UUID targetUUID) {
        return members.containsKey(targetUUID) && members.get(targetUUID).equals(RanksManager.BANNED_RANK);
    }

    /**
     * Returns whether the island is a spawn or not.
     * @return {@code true} if the island is a spawn, {@code false} otherwise.
     */
    public boolean isSpawn() {
        return spawn;
    }

    /**
     * Checks if a location is within this island's protected area.
     *
     * @param target location to check, not null
     * @return {@code true} if this location is within this island's protected area, {@code false} otherwise.
     */
    public boolean onIsland(@NonNull Location target) {
        return Util.sameWorld(world, target.getWorld()) && target.getBlockX() >= getMinProtectedX() && target.getBlockX() < (getMinProtectedX() + protectionRange * 2) && target.getBlockZ() >= getMinProtectedZ() && target.getBlockZ() < (getMinProtectedZ() + protectionRange * 2);
    }

    /**
     * Returns a {@link BoundingBox} of this island's protected area.
     * @return a {@link BoundingBox} of this island's protected area.
     * @since 1.5.2
     */
    public BoundingBox getProtectionBoundingBox() {
        return new BoundingBox(getMinProtectedX(), 0.0D, getMinProtectedZ(), getMaxProtectedX()-1.0D, world.getMaxHeight(), getMaxProtectedZ()-1.0D);
    }

    /**
     * Removes a player from the team member map. Generally, you should
     * use {@link world.bentobox.bentobox.managers.IslandsManager#removePlayer(World, UUID)}
     * @param playerUUID - uuid of player
     */
    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
        setChanged();
    }

    /**
     * @param center the center to set
     */
    public void setCenter(@Nullable Location center) {
        if (center != null) {
            this.world = center.getWorld();
        }
        this.center = center;
        setChanged();
    }

    /**
     * @param createdDate - the createdDate to sets
     */
    public void setCreatedDate(long createdDate){
        this.createdDate = createdDate;
        setChanged();
    }

    /**
     * Set the Island Guard flag rank
     * @param flag - flag
     * @param value - Use RanksManager settings, e.g. RanksManager.MEMBER
     */
    public void setFlag(Flag flag, int value){
        flags.put(flag, value);
        setChanged();
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(Map<Flag, Integer> flags) {
        this.flags = flags;
        setChanged();
    }

    /**
     * Resets the flags to their default as set in config.yml for this island.
     * If flags are missing from the config, the default hard-coded value is used and set
     */
    public void setFlagsDefaults() {
        BentoBox plugin = BentoBox.getInstance();
        Map<Flag, Integer> result = new HashMap<>();
        plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(Flag.Type.PROTECTION))
        .forEach(f -> result.put(f, plugin.getIWM().getDefaultIslandFlags(world).getOrDefault(f, f.getDefaultRank())));
        plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(Flag.Type.SETTING))
        .forEach(f -> result.put(f, plugin.getIWM().getDefaultIslandSettings(world).getOrDefault(f, f.getDefaultRank())));
        this.setFlags(result);
        setChanged();
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
        setChanged();
    }

    /**
     * @param members the members to set
     */
    public void setMembers(Map<UUID, Integer> members) {
        this.members = members;
        setChanged();
    }

    /**
     * Sets the display name of this Island.
     * <br/><br/>
     * An empty String or {@code null} will remove the display name.
     * @param name The display name to set.
     */
    public void setName(String name){
        this.name = (name != null && !name.equals("")) ? name : null;
        setChanged();
    }

    /**
     * Sets the owner of the island.
     * @param owner the island owner - the owner to set
     */
    public void setOwner(@Nullable UUID owner){
        if (this.owner == owner) {
            return; //No need to update anything
        }

        this.owner = owner;
        if (owner == null) {
            log(new LogEntry.Builder("UNOWNED").build());
            return;
        }
        // Defensive code: demote any previous owner
        for (Entry<UUID, Integer> en : members.entrySet()) {
            if (en.getValue().equals(RanksManager.OWNER_RANK)) {
                setRank(en.getKey(), RanksManager.MEMBER_RANK);
            }
        }
        setRank(owner, RanksManager.OWNER_RANK);
        setChanged();
    }

    /**
     * @param protectionRange the protectionRange to set
     */
    public void setProtectionRange(int protectionRange) {
        this.protectionRange = protectionRange;
        this.updateMaxEverProtectionRange();
        setChanged();
    }

    /**
     * Updates the maxEverProtectionRange based on the current protectionRange
     */
    public void updateMaxEverProtectionRange() {
        // Ratchet up the maximum protection range
        // Distance from maxes
        int diffMinX = Math.abs(Objects.requireNonNull(getCenter()).getBlockX() - this.getMinProtectedX());
        int diffMaxX = Math.abs(getCenter().getBlockX() - this.getMaxProtectedX());
        int diffMinZ = Math.abs(getCenter().getBlockZ() - this.getMinProtectedZ());
        int diffMaxZ = Math.abs(getCenter().getBlockZ() - this.getMaxProtectedZ());
        if (diffMinX > this.maxEverProtectionRange) {
            this.maxEverProtectionRange = diffMinX;
        }
        if (diffMaxX > this.maxEverProtectionRange) {
            this.maxEverProtectionRange = diffMaxX;
        }
        if (diffMinZ > this.maxEverProtectionRange) {
            this.maxEverProtectionRange = diffMinZ;
        }
        if (diffMaxZ > this.maxEverProtectionRange) {
            this.maxEverProtectionRange = diffMaxZ;
        }

    }

    /**
     * @param purgeProtected - if the island is protected from the Purge
     */
    public void setPurgeProtected(boolean purgeProtected){
        this.purgeProtected = purgeProtected;
        setChanged();
    }

    /**
     * Sets the island range.
     * This method should <u><strong>NEVER</strong></u> be used except for testing purposes.
     * <br>
     * The range value is a copy of {@link WorldSettings#getIslandDistance()} made when the Island
     * got created in order to allow easier access to this value and must therefore remain
     * <u><strong>AS IS</strong></u>.
     * @param range the range to set
     * @see #setProtectionRange(int)
     */
    public void setRange(int range){
        this.range = range;
        setChanged();
    }

    /**
     * Set user's rank to an arbitrary rank value
     * @param user the User
     * @param rank rank value
     */
    public void setRank(User user, int rank) {
        setRank(user.getUniqueId(), rank);
        setChanged();
    }

    /**
     * Sets player's rank to an arbitrary rank value.
     * Calling this method won't call the {@link IslandEvent.IslandRankChangeEvent}.
     * @param uuid UUID of the player
     * @param rank rank value
     * @since 1.1
     */
    public void setRank(@Nullable UUID uuid, int rank) {
        if (uuid == null) {
            return; // Defensive code
        }
        members.put(uuid, rank);
        setChanged();
    }

    /**
     * @param ranks the ranks to set
     */
    public void setRanks(Map<UUID, Integer> ranks) {
        members = ranks;
        setChanged();
    }

    /**
     * Sets whether this island is a spawn or not.
     * <br/>
     * If {@code true}, the members and the owner will be removed from this island.
     * The flags will also be resetted to default values.
     * @param isSpawn {@code true} if the island is a spawn, {@code false} otherwise.
     */
    public void setSpawn(boolean isSpawn){
        if (spawn == isSpawn) {
            return; // No need to update anything
        }

        spawn = isSpawn;
        if (isSpawn) {
            setOwner(null);
            members.clear();
            setFlagsDefaults();
            setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
        }
        log(new LogEntry.Builder("SPAWN").data("value", String.valueOf(isSpawn)).build());
        setChanged();
    }

    /**
     * Get the default spawn location for this island. Note that this may only be valid
     * after the initial pasting because the player can change the island after that point
     * @return the spawnPoint
     */
    public Map<Environment, Location> getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * Set when island is pasted
     * @param spawnPoint the spawnPoint to set
     */
    public void setSpawnPoint(Map<Environment, Location> spawnPoint) {
        this.spawnPoint = spawnPoint;
        setChanged();
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
        setChanged();
    }

    /**
     * @param updatedDate - the updatedDate to sets
     */
    public void setUpdatedDate(long updatedDate){
        this.updatedDate = updatedDate;
        setChanged();
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
        setChanged();
    }

    /**
     * Shows info of this island to this user.
     * @param user the User who is requesting it
     * @return always true
     */
    public boolean showInfo(User user) {
        BentoBox plugin = BentoBox.getInstance();
        user.sendMessage("commands.admin.info.title");
        user.sendMessage("commands.admin.info.island-uuid", "[uuid]", this.getUniqueId());
        if (getOwner() == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(getOwner()), "[uuid]", getOwner().toString());

            // Fixes #getLastPlayed() returning 0 when it is the owner's first connection.
            long lastPlayed = (Bukkit.getServer().getOfflinePlayer(getOwner()).getLastPlayed() != 0) ?
                    Bukkit.getServer().getOfflinePlayer(getOwner()).getLastPlayed() : Bukkit.getServer().getOfflinePlayer(getOwner()).getFirstPlayed();
            user.sendMessage("commands.admin.info.last-login","[date]", new Date(lastPlayed).toString());

            user.sendMessage("commands.admin.info.deaths", "[number]", String.valueOf(plugin.getPlayers().getDeaths(getWorld(), getOwner())));
            String resets = String.valueOf(plugin.getPlayers().getResets(getWorld(), getOwner()));
            String total = plugin.getIWM().getResetLimit(getWorld()) < 0 ? "Unlimited" : String.valueOf(plugin.getIWM().getResetLimit(getWorld()));
            user.sendMessage("commands.admin.info.resets-left", "[number]", resets, "[total]", total);
            // Show team members
            showMembers(user);
        }
        Vector location = getProtectionCenter().toVector();
        user.sendMessage("commands.admin.info.island-protection-center", TextVariables.XYZ, Util.xyz(location));
        user.sendMessage("commands.admin.info.island-center", TextVariables.XYZ, Util.xyz(getCenter().toVector()));
        user.sendMessage("commands.admin.info.island-coords", "[xz1]", Util.xyz(new Vector(this.getMinX(), 0, getMinZ())), "[xz2]", Util.xyz(new Vector(this.getMaxX(), 0, getMaxZ())));
        user.sendMessage("commands.admin.info.protection-range", "[range]", String.valueOf(getProtectionRange()));
        user.sendMessage("commands.admin.info.max-protection-range", "[range]", String.valueOf(getMaxEverProtectionRange()));
        user.sendMessage("commands.admin.info.protection-coords", "[xz1]", Util.xyz(new Vector(this.getMinProtectedX(), 0, getMinProtectedZ())), "[xz2]", Util.xyz(new Vector(this.getMaxProtectedX(), 0, getMaxProtectedZ())));
        if (spawn) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        if (!getBanned().isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            getBanned().forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME, plugin.getPlayers().getName(u)));
        }
        if (getPurgeProtected()) {
            user.sendMessage("commands.admin.info.purge-protected");
        }
        return true;
    }

    /**
     * Shows the members of this island to this user.
     * @param user the User who is requesting it
     */
    public void showMembers(User user) {
        BentoBox plugin = BentoBox.getInstance();
        user.sendMessage("commands.admin.info.team-members-title");
        members.forEach((u, i) -> {
            if (owner.equals(u)) {
                user.sendMessage("commands.admin.info.team-owner-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            } else if (i > RanksManager.VISITOR_RANK){
                user.sendMessage("commands.admin.info.team-member-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            }
        });
    }

    /**
     * Toggles a settings flag
     * @param flag - flag
     */
    public void toggleFlag(Flag flag) {
        if (flag.getType().equals(Flag.Type.SETTING) || flag.getType().equals(Flag.Type.WORLD_SETTING)) {
            setSettingsFlag(flag, !isAllowed(flag));
        }
        setChanged();
    }

    /**
     * Sets the state of a settings flag
     * @param flag - flag
     * @param state - true or false
     */
    public void setSettingsFlag(Flag flag, boolean state) {
        if (flag.getType().equals(Flag.Type.SETTING) || flag.getType().equals(Flag.Type.WORLD_SETTING)) {
            flags.put(flag, state ? 1 : -1);
        }
        setChanged();
    }

    /**
     * Set the spawn location for this island type
     * @param islandType - island type
     * @param l - location
     */
    public void setSpawnPoint(Environment islandType, Location l) {
        spawnPoint.put(islandType, l);
        setChanged();
    }

    /**
     * Get the spawn point for this island type
     * @param islandType - island type
     * @return - location or null if one does not exist
     */
    @Nullable
    public Location getSpawnPoint(Environment islandType) {
        return spawnPoint.get(islandType);
    }

    /**
     * Removes all of a specified rank from the member list
     * @param rank rank value
     */
    public void removeRank(Integer rank) {
        members.values().removeIf(rank::equals);
        setChanged();
    }

    /**
     * Gets the history of the island.
     * @return the list of {@link LogEntry} for this island.
     */
    public List<LogEntry> getHistory() {
        return history;
    }

    /**
     * Adds a {@link LogEntry} to the history of this island.
     * @param logEntry the LogEntry to add.
     */
    public void log(LogEntry logEntry) {
        history.add(logEntry);
        setChanged();
    }

    /**
     * Sets the history of the island.
     * @param history the list of {@link LogEntry} to set for this island.
     */
    public void setHistory(List<LogEntry> history) {
        this.history = history;
        setChanged();
    }

    /**
     * @return the doNotLoad
     */
    public boolean isDoNotLoad() {
        return doNotLoad;
    }

    /**
     * @param doNotLoad the doNotLoad to set
     */
    public void setDoNotLoad(boolean doNotLoad) {
        this.doNotLoad = doNotLoad;
        setChanged();
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param deleted the deleted to set
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        setChanged();
    }

    /**
     * Returns the name of the {@link world.bentobox.bentobox.api.addons.GameModeAddon GameModeAddon} this island is handled by.
     * @return the name of the {@link world.bentobox.bentobox.api.addons.GameModeAddon GameModeAddon} this island is handled by.
     * @since 1.5.0
     */
    public String getGameMode() {
        return gameMode;
    }

    /**
     * Sets the name of the {@link world.bentobox.bentobox.api.addons.GameModeAddon GameModeAddon} this island is handled by.
     * Note this has no effect over the actual location of the island, however this may cause issues with addons using this data.
     * @since 1.5.0
     */
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
        setChanged();
    }

    /**
     * Checks whether this island has its nether island generated or not.
     * @return {@code true} if this island has its nether island generated, {@code false} otherwise.
     * @since 1.5.0
     */
    public boolean hasNetherIsland(){
        IslandWorldManager iwm = BentoBox.getInstance().getIWM();
        return iwm.isNetherGenerate(getWorld()) && iwm.isNetherIslands(getWorld()) &&
                iwm.getNetherWorld(getWorld()) != null &&
                !getCenter().toVector().toLocation(iwm.getNetherWorld(getWorld())).getBlock().getType().equals(Material.AIR);
    }

    /**
     * Checks whether this island has its end island generated or not.
     * @return {@code true} if this island has its end island generated, {@code false} otherwise.
     * @since 1.5.0
     */
    public boolean hasEndIsland(){
        IslandWorldManager iwm = BentoBox.getInstance().getIWM();
        return iwm.isEndGenerate(getWorld()) && iwm.isEndIslands(getWorld()) &&
                iwm.getEndWorld(getWorld()) != null &&
                !getCenter().toVector().toLocation(iwm.getEndWorld(getWorld())).getBlock().getType().equals(Material.AIR);
    }


    /**
     * Checks if a flag is on cooldown. Only stored in memory so a server restart will reset the cooldown.
     * @param flag - flag
     * @return true if on cooldown, false if not
     * @since 1.6.0
     */
    public boolean isCooldown(Flag flag) {
        if (cooldowns.containsKey(flag) && cooldowns.get(flag) > System.currentTimeMillis()) {
            return true;
        }
        cooldowns.remove(flag);
        setChanged();
        return false;
    }

    /**
     * Sets a cooldown for this flag on this island.
     * @param flag - Flag to cooldown
     */
    public void setCooldown(Flag flag) {
        cooldowns.put(flag, flag.getCooldown() * 1000 + System.currentTimeMillis());
        setChanged();
    }

    /**
     * @return the cooldowns
     */
    public Map<Flag, Long> getCooldowns() {
        return cooldowns;
    }

    /**
     * @param cooldowns the cooldowns to set
     */
    public void setCooldowns(Map<Flag, Long> cooldowns) {
        this.cooldowns = cooldowns;
        setChanged();
    }

    /**
     * @return the commandRanks
     */
    public Map<String, Integer> getCommandRanks() {
        return commandRanks;
    }

    /**
     * @param commandRanks the commandRanks to set
     */
    public void setCommandRanks(Map<String, Integer> commandRanks) {
        this.commandRanks = commandRanks;
        setChanged();
    }

    /**
     * Get the rank required to run command on this island.
     * The command must have been registered with a rank.
     * @param command - the string given by {@link CompositeCommand#getUsage()}
     * @return Rank value required, or if command is not set {@link RanksManager#OWNER_RANK}
     */
    public int getRankCommand(String command) {
        return commandRanks == null ? RanksManager.OWNER_RANK : commandRanks.getOrDefault(command, RanksManager.OWNER_RANK);
    }

    /**
     *
     * @param command - the string given by {@link CompositeCommand#getUsage()}
     * @param rank value as used by {@link RanksManager}
     */
    public void setRankCommand(String command, int rank) {
        if (this.commandRanks == null) this.commandRanks = new HashMap<>();
        this.commandRanks.put(command, rank);
        setChanged();
    }

    /**
     * Returns whether this Island is currently reserved or not.
     * If {@code true}, this means no blocks, except a bedrock one at the center of the island, exist.
     * @return {@code true} if this Island is reserved, {@code false} otherwise.
     * @since 1.6.0
     */
    public boolean isReserved() {
        return reserved != null && reserved;
    }

    /**
     * @param reserved the reserved to set
     * @since 1.6.0
     */
    public void setReserved(boolean reserved) {
        this.reserved = reserved;
        setChanged();
    }

    /**
     * @return the metaData
     * @since 1.15.5
     */
    @Override
    public Optional<Map<String, MetaDataValue>> getMetaData() {
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        return Optional.of(metaData);
    }

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     */
    @Override
    public void setMetaData(Map<String, MetaDataValue> metaData) {
        this.metaData = metaData;
        setChanged();
    }

    /**
     * @return changed state
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Indicates the class has been changed
     */
    public void setChanged() {
        this.changed = true;
    }

    /**
     * @param changed the changed to set
     */
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /**
     * Get the center location of the protection zone.
     * This can be anywhere within the island space and can move.
     * Unless explicitly set, it will return the same as {@link #getCenter()}.
     * @return a clone of the protection center location
     * @since 1.16.0
     */
    @Nullable
    public Location getProtectionCenter() {
        return location == null ? getCenter() : location.clone();
    }

    /**
     * Sets the protection center location of the island within the island space.
     * @param location the location to set
     * @throws IOException if the location is not in island space
     * @since 1.16.0
     */
    public void setProtectionCenter(Location location) throws IOException {
        if (!this.inIslandSpace(location)) {
            throw new IOException("Location must be in island space");
        }
        this.location = location;
        this.updateMaxEverProtectionRange();
        setChanged();
    }

    /**
     * @return the homes
     * @since 1.16.0
     */
    @NonNull
    public Map<String, Location> getHomes() {
        if (homes == null) {
            homes = new HashMap<>();
        }
        return homes;
    }

    /**
     * @return the homes
     * @since 1.16.0
     */
    @Nullable
    public Location getHome(String name) {
        return getHomes().get(name.toLowerCase());
    }

    /**
     * @param homes the homes to set
     * @since 1.16.0
     */
    public void setHomes(Map<String, Location> homes) {
        this.homes = homes;
        setChanged();
    }

    /**
     * @param name the name of the home
     * @since 1.16.0
     */
    public void addHome(String name, Location location) {
        getHomes().put(name.toLowerCase(), location);
        setChanged();
    }

    /**
     * Remove a named home from this island
     * @param name - home name to remove
     * @return true if home removed successfully
     * @since 1.16.0
     */
    public boolean removeHome(String name) {
        setChanged();
        return getHomes().remove(name.toLowerCase()) != null;
    }

    /**
     * Rename a home
     * @param oldName - old name of home
     * @param newName - new name of home
     * @return true if successful, false if oldName does not exist, already exists
     * @since 1.16.0
     */
    public boolean renameHome(String oldName, String newName) {
        if (getHomes().containsKey(oldName.toLowerCase()) && !getHomes().containsKey(newName.toLowerCase())) {
            this.addHome(newName, this.getHome(oldName));
            this.removeHome(oldName);
            return true;
        }
        return false;
    }

    /**
     * @return the maxHomes. If null, then the world default should be used.
     * @since 1.16.0
     */
    @Nullable
    public Integer getMaxHomes() {
        return maxHomes;
    }

    /**
     * @param maxHomes the maxHomes to set. If null then the world default will be used.
     * @since 1.16.0
     */
    public void setMaxHomes(@Nullable Integer maxHomes) {
        this.maxHomes = maxHomes;
        setChanged();
    }

    /**
     * @return the maxMembers
     * @since 1.16.0
     */
    public Map<Integer, Integer> getMaxMembers() {
        if (maxMembers == null) {
            maxMembers = new HashMap<>();
        }
        return maxMembers;
    }

    /**
     * @param maxMembers the maxMembers to set
     * @since 1.16.0
     */
    public void setMaxMembers(Map<Integer, Integer> maxMembers) {
        this.maxMembers = maxMembers;
        setChanged();
    }

    /**
     * Get the maximum number of island members
     * @param rank island rank value from {@link RanksManager}
     * @return the maxMembers for the rank given - if null then the world default should be used. Negative values = unlimited.
     * @since 1.16.0
     */
    @Nullable
    public Integer getMaxMembers(int rank) {
        return getMaxMembers().get(rank);
    }


    /**
     * Set the maximum number of island members
     * @param rank island rank value from {@link RanksManager}
     * @param maxMembers the maxMembers to set. If null then the world default applies. Negative values = unlimited.
     * @since 1.16.0
     */
    public void setMaxMembers(int rank, Integer maxMembers) {
        getMaxMembers().put(rank, maxMembers);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Island [changed=" + changed + ", deleted=" + deleted + ", uniqueId=" + uniqueId + ", center=" + center
                + ", location=" + location + ", range=" + range + ", protectionRange=" + protectionRange
                + ", maxEverProtectionRange=" + maxEverProtectionRange + ", world=" + world + ", gameMode=" + gameMode
                + ", name=" + name + ", createdDate=" + createdDate + ", updatedDate=" + updatedDate + ", owner="
                + owner + ", members=" + members + ", maxMembers=" + maxMembers + ", spawn=" + spawn
                + ", purgeProtected=" + purgeProtected + ", flags=" + flags + ", history=" + history
                + ", levelHandicap=" + levelHandicap + ", spawnPoint=" + spawnPoint + ", doNotLoad=" + doNotLoad
                + ", cooldowns=" + cooldowns + ", commandRanks=" + commandRanks + ", reserved=" + reserved
                + ", metaData=" + metaData + ", homes=" + homes + ", maxHomes=" + maxHomes + "]";
    }



}
