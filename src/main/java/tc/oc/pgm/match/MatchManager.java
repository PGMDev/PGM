package tc.oc.pgm.match;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.PGM;
import tc.oc.pgm.events.CycleEvent;
import tc.oc.pgm.events.MapArchiveEvent;
import tc.oc.pgm.events.SetNextMapEvent;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.MapLoader;
import tc.oc.pgm.map.MapNotFoundException;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.util.FileUtils;
import tc.oc.util.RandomUtils;
import tc.oc.util.logging.ClassLogger;
import tc.oc.world.NMSHacks;

public class MatchManager {
  /** Matches that are currently running. */
  private final Map<World, Match> matches = new HashMap<World, Match>();

  /** List of loaded maps that could be loaded. */
  private final MapLibrary mapLibrary;

  private final MapLoader mapLoader;

  /** Custom set next map. */
  private PGMMap nextMap = null;

  /** FIXME: Placeholder until we get the database set up. */
  private int matchId = 0;

  // Inherited
  private final PGM parent;
  private final Server server;
  private final Logger log;
  private final WorldManager mapManager;

  /** Creates a new map manager with a specified map rotation. */
  public MatchManager(
      PGM parent, MapLibrary mapLibrary, MapLoader mapLoader, WorldManager mapManager)
      throws MapNotFoundException {
    this.log = ClassLogger.get(parent.getLogger(), getClass());
    this.parent = parent;
    this.server = parent.getServer();
    this.mapLibrary = mapLibrary;
    this.mapLoader = mapLoader;
    this.mapManager = mapManager;

    loadNewMaps();
  }

  public int getMatchId() {
    return this.matchId;
  }

  /** Gets the currently loaded maps. */
  public Collection<PGMMap> getMaps() {
    return this.mapLibrary.getMaps();
  }

  public MapLibrary getMapLibrary() {
    return this.mapLibrary;
  }

  /** Gets the match being played on a world, or null. */
  public Match getMatch(World world) {
    return this.matches.get(world);
  }

  public Match needMatch(World world) {
    Match match = getMatch(world);
    if (match == null) {
      throw new IllegalStateException("No match available for world " + world.getName());
    }
    return match;
  }

  public Collection<Match> getCurrentMatches() {
    return this.matches.values();
  }

  /** Gets the current match that is running. */
  public @Nullable Match getCurrentMatch() {
    if (matches.isEmpty()) {
      return null;
    } else if (this.matches.size() == 1) {
      return this.matches.values().iterator().next();
    } else {
      throw new IllegalStateException(
          "Called getCurrentMatch while multiple matches exist (probably during cycle)");
    }
  }

  public Match needCurrentMatch() {
    if (matches.isEmpty()) {
      throw new IllegalStateException("Tried to get the current match when no match was loaded");
    } else {
      return getCurrentMatch();
    }
  }

  /** Gets the current match for a given command sender. */
  public Match getCurrentMatch(CommandSender sender) {
    if (sender instanceof Entity) {
      return this.getMatch(((Entity) sender).getWorld());
    } else {
      return this.getCurrentMatch();
    }
  }

  /** Gets the MatchPlayer instance of a player by name. */
  public @Nullable MatchPlayer getPlayer(@Nullable OfflinePlayer player) {
    return player != null && player.isOnline() ? getPlayer(player.getPlayer()) : null;
  }

  /** Gets the MatchPlayer instance of a Bukkit Player instance. */
  public @Nullable MatchPlayer getPlayer(@Nullable Player player) {
    if (player == null) return null;
    Match match = this.getMatch(player.getWorld());
    return match == null ? null : match.getPlayer(player);
  }

  public @Nullable MatchPlayer getPlayer(@Nullable Entity player) {
    return player instanceof Player ? getPlayer((Player) player) : null;
  }

  public @Nullable MatchPlayer getPlayer(@Nullable MatchPlayerState state) {
    return state == null ? null : this.getPlayer(state.getPlayerId());
  }

  public @Nullable MatchPlayer getPlayer(@Nullable UUID playerId) {
    return playerId == null ? null : this.getPlayer(Bukkit.getPlayer(playerId));
  }

  public @Nullable MatchPlayerState getPlayerState(@Nullable Player player) {
    if (player == null) return null;
    MatchPlayer matchPlayer = this.getPlayer(player);
    return matchPlayer == null ? null : matchPlayer.getState();
  }

  public @Nullable MatchPlayerState getPlayerState(@Nullable Entity entity) {
    return entity instanceof Player ? this.getPlayerState((Player) entity) : null;
  }

  public @Nullable ParticipantState getParticipantState(@Nullable Player player) {
    if (player == null) return null;
    MatchPlayer matchPlayer = this.getPlayer(player);
    return matchPlayer == null ? null : matchPlayer.getParticipantState();
  }

  public @Nullable ParticipantState getParticipantState(@Nullable Entity entity) {
    return entity instanceof Player ? this.getParticipantState((Player) entity) : null;
  }

  public @Nullable ParticipantState getParticipantState(@Nullable UUID playerId) {
    if (playerId == null) return null;
    MatchPlayer matchPlayer = this.getPlayer(playerId);
    return matchPlayer == null ? null : matchPlayer.getParticipantState();
  }

  public boolean canInteract(Object obj) {
    return MatchPlayers.canInteract(this.resolvePlayer(obj));
  }

  public boolean cannotInteract(Object obj) {
    return MatchPlayers.cannotInteract(this.resolvePlayer(obj));
  }

  private MatchPlayer resolvePlayer(Object obj) {
    if (obj instanceof MatchPlayer) {
      return (MatchPlayer) obj;
    } else if (obj instanceof Player) {
      return this.getPlayer((Player) obj);
    } else {
      return null;
    }
  }

  private Set<PGMMap> loadNewMaps() throws MapNotFoundException {
    Set<Path> added = new HashSet<>(), updated = new HashSet<>(), removed = new HashSet<>();
    List<PGMMap> maps = mapLoader.loadNewMaps(mapLibrary.getMapsByPath(), added, updated, removed);
    mapLibrary.removeMaps(removed);
    Set<PGMMap> newMaps = mapLibrary.addMaps(maps);

    log.info("Loaded " + newMaps.size() + " maps");

    if (mapLibrary.getMaps().isEmpty()) {
      throw new MapNotFoundException();
    }

    return newMaps;
  }

  public Set<PGMMap> loadMaps() throws MapNotFoundException {
    return loadNewMaps();
  }
  /**
   * Gets the next map that will be loaded at this point in time. If a map had been specified
   * explicitly (setNextMap) that will be returned, otherwise the next map in the rotation will be
   * returned.
   *
   * @return Next map that would be loaded at this point in time.
   */
  public PGMMap getNextMap() {
    if (this.nextMap == null) {
      setNextMap(RandomUtils.element(new Random(), mapLibrary.getMaps()));
    }
    return this.nextMap;
  }

  /**
   * Specified an explicit map for the next cycle.
   *
   * @param map to be loaded next.
   */
  public void setNextMap(PGMMap map) {
    if (map != nextMap) {
      this.nextMap = map;
      Bukkit.getPluginManager().callEvent(new SetNextMapEvent(map));
    }
  }

  private String nextWorldName() {
    return "match-" + ++matchId;
  }

  /**
   * Cycle to the next map in the rotation
   *
   * @param oldMatch The current match, if any
   * @param retryRotation Try every map in the rotation until one loads successfully
   * @param retryLibrary Try every map in the library, after trying the entire rotation
   * @return The new match, or null if no map could be loaded
   */
  public @Nullable Match cycle(
      @Nullable Match oldMatch, boolean retryRotation, boolean retryLibrary) {
    // Match unload also does this, but doing it earlier avoids some problems.
    // Specifically, RestartCountdown cannot cancel itself during a cycle.
    if (oldMatch != null) {
      oldMatch.getCountdownContext().cancelAll();
    }

    String name = nextWorldName();
    Set<PGMMap> failed = new HashSet<>(); // Don't try any map more than once

    // Try to load a rotation map
    int maxCycles = mapLibrary.getMaps().size();
    for (int cycles = 0; cycles < maxCycles; cycles++) {
      PGMMap map = getNextMap();

      if (!failed.contains(map)) {
        Match match = cycleSafe(oldMatch, map, name);
        if (match != null) return match;
      }

      // If retryRotation is false, give up after the first failure
      if (!retryRotation) return null;

      failed.add(map);
    }

    // If all rotation maps failed, and we're not allowed to try non-rotation maps, give up
    if (!retryLibrary) return null;

    // Try every map in the library
    for (PGMMap map : mapLibrary.getMaps()) {
      if (!failed.contains(map)) {
        Match match = cycleSafe(oldMatch, map, name);
        if (match != null) return match;
      }
      failed.add(map);
    }

    return null;
  }

  /**
   * Call {@link #cycleUnsafe(Match, PGMMap, String)} and log any exceptions to the map logger.
   *
   * @return the new match, or null if loading failed
   */
  private @Nullable Match cycleSafe(@Nullable Match oldMatch, PGMMap newMap, String worldName) {
    try {
      return this.cycleUnsafe(oldMatch, newMap, worldName);
    } catch (MapNotFoundException e) {
      // Maps are sometimes removed, must handle it gracefully
      log.warning("Skipping deleted map " + newMap.getName());
      try {
        loadMaps();
      } catch (MapNotFoundException not) {
        log.severe("No maps could be loaded, server cannot cycle");
      }
      return null;
    } catch (ModuleLoadException e) {
      if (e.getModule() != null) {
        newMap
            .getLogger()
            .log(
                Level.SEVERE,
                "Exception loading module " + e.getModule().getName() + ": " + e.getMessage(),
                e);
      } else {
        newMap.getLogger().log(Level.SEVERE, "Exception loading map: " + e.getMessage(), e);
      }
    } catch (Throwable e) {
      newMap.getLogger().log(Level.SEVERE, "Exception loading map: " + e.getMessage(), e);
    }
    return null;
  }

  /**
   * Creates and loads a new {@link Match} on the given map, optionally unloading an old match and
   * transferring all players to the new one.
   *
   * @param oldMatch if given, this match is unloaded and all players are transferred to the new
   *     match
   * @param newMap the map to load for the new match
   * @param worldName name of the {@link World} that will be created for the new match
   * @return the newly loaded {@link Match}
   * @throws Throwable generally, any exceptions thrown during loading/unloading are propagated
   */
  private Match cycleUnsafe(@Nullable Match oldMatch, PGMMap newMap, String worldName)
      throws Throwable {
    this.log.info("Cycling to " + newMap.toString());

    newMap.reload(true);

    if (oldMatch != null) oldMatch.end();

    Match newMatch = this.loadMatch(newMap, worldName);

    if (oldMatch != null) {
      Set<Player> players = new HashSet<>(oldMatch.getPlayers().size());
      for (MatchPlayer matchPlayer : oldMatch.getPlayers()) {
        players.add(matchPlayer.getBukkit());
      }

      for (Player player : players) {
        NMSHacks.forceRespawn(player);
        player.teleport(newMatch.getWorld().getSpawnLocation());
        player.setArrowsStuck(0);
      }

      oldMatch.removeAllPlayers();

      for (Player player : players) {
        newMatch.addPlayer(player);
      }

      newMatch.refreshPlayerGameModes();

      this.unloadMatch(oldMatch);
    }

    this.server.getPluginManager().callEvent(new CycleEvent(newMatch, oldMatch));
    this.log.info("Loaded " + newMap.toString());
    this.nextMap = null;

    return newMatch;
  }

  /**
   * Try to load a match. This will take care of copying and loading the new world into Bukkit. May
   * throw an assortment of exceptions if something goes wrong.
   *
   * @param map Map to load.
   * @param worldName Destination for the new world copy. Will override if it exists.
   */
  private Match loadMatch(PGMMap map, String worldName) throws Throwable {
    File destination = new File(this.server.getWorldContainer(), worldName);
    if (destination.exists()) {
      FileUtils.delete(destination);
    }
    if (!map.getContext().isPresent()) {
      map.reload(true);
    }
    this.mapManager.copyWorld(
        map.getContext().get().needModule(TerrainModule.class).getWorldFolder(),
        destination); // may throw

    World world = this.mapManager.createWorld(worldName, map); // may throw
    Match match = new Match(this.parent, map, world);
    this.matches.put(world, match);

    try {
      match.load();
      return match;
    } catch (Throwable e) {
      this.matches.remove(world);
      this.mapManager.unloadWorld(world);
      throw e;
    }
  }

  /** Unload match modules, unload the world, and archive it */
  private void unloadMatch(Match match) {
    match.unload();

    this.mapManager.unloadWorld(match.getWorld());

    MapArchiveEvent archiveEvent = new MapArchiveEvent(match, null);
    this.server.getPluginManager().callEvent(archiveEvent);
    this.mapManager.archive(match.getWorld().getName(), archiveEvent.getOutputDirectory());

    this.matches.remove(match.getWorld());

    // Don't delete the map context if another match is using the map
    for (Match matchCheck : matches.values()) {
      if (matchCheck.getMap() == match.getMap()) {
        return;
      }
    }
    match.getMap().deleteContext();
  }

  /**
   * Force all {@link Match}es to end and unload. This should only be called immediately before the
   * server shuts down as it will leave PGM in a completely dysfunctional state.
   */
  public void unloadAllMatches() {
    for (Match match : ImmutableSet.copyOf(this.matches.values())) {
      match.end();
      match.removeAllPlayers();
      this.unloadMatch(match);
    }
  }
}
