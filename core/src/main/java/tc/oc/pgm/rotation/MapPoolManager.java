package tc.oc.pgm.rotation;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.events.MapPoolAdjustEvent;
import tc.oc.pgm.util.TimeUtils;

/**
 * Manages all the existing {@link MapPool}s, as for maintaining their order, and updating the one
 * {@link PGM} will use after every match depending on the player count (Dynamic Rotations)
 */
public class MapPoolManager implements MapOrder {

  private Logger logger;

  private File mapPoolsFile;
  private FileConfiguration mapPoolFileConfig;

  private Map<MapPool, MapActivity> mapPools = Maps.newHashMap();
  private MapPool activeMapPool;

  /* If a time limit is added via /setpool <name> -t [time], then after this duration the map pool will revert automatically */
  private Duration poolTimeLimit = null;
  private Instant poolStartTime = null;

  private int matchCountLimit = 0; // The limit for when to revert to dynamic pools
  private int matchCount = 0; // # of completed matches since start of pool

  /** When a {@link MapInfo} is manually set next, it overrides the rotation order * */
  private MapInfo overriderMap;

  /** Options related to voting pools, allows for custom voting @see {@link VotingPool} * */
  private CustomVotingPoolOptions options;

  private Datastore database;

  public MapPoolManager(Logger logger, File mapPoolsFile, Datastore database) {
    this.logger = logger;
    this.mapPoolsFile = mapPoolsFile;
    this.database = database;
    this.options = new CustomVotingPoolOptions();

    if (!mapPoolsFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(PGM.get().getResource("map-pools.yml"), mapPoolsFile);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create the map-pools.yml file", e);
        return;
      }
    }

    reload();
  }

  public @Nullable String getNextMapForPool(String poolName) {
    String mapName = database.getMapActivity(poolName).getMapName();
    if (mapName != null) {
      logger.log(
          Level.INFO,
          String.format("%s was found in map activity as the next map (%s).", mapName, poolName));
    }
    return mapName;
  }

  @Override
  public void reload() {
    this.mapPoolFileConfig = YamlConfiguration.loadConfiguration(mapPoolsFile);
    loadMapPools();
  }

  private int loadMapPools() {
    List<MapPool> pools =
        mapPoolFileConfig.getConfigurationSection("pools").getKeys(false).stream()
            .map(key -> MapPool.of(this, mapPoolFileConfig, key))
            .filter(MapPool::isEnabled)
            .collect(Collectors.toList());

    List<MapActivity> activity =
        pools.stream()
            .map(MapPool::getName)
            .map(database::getMapActivity)
            .collect(Collectors.toList());

    this.mapPools.clear(); // For reloads

    pools.forEach(
        pool ->
            mapPools.put(
                pool,
                activity.stream()
                    .filter(a -> a.getPoolName().equalsIgnoreCase(pool.getName()))
                    .findAny()
                    .orElse(null)));

    Optional<MapActivity> lastActive =
        mapPools.values().stream().filter(ma -> ma.isActive()).findFirst();
    if (lastActive.isPresent()) {
      activeMapPool = getMapPoolByName(lastActive.get().getPoolName());
    }

    if (activeMapPool == null) {
      logger.log(Level.WARNING, "No active map pool was found, defaulting to first dynamic pool.");
      activeMapPool =
          mapPools.keySet().stream().sorted().filter(mp -> mp.isDynamic()).findFirst().orElse(null);
      if (activeMapPool == null) {
        logger.log(Level.SEVERE, "Failed to find any dynamic map pool!");
      }
    } else {
      logger.log(Level.INFO, "Resuming last active map pool (" + activeMapPool.getName() + ")");
    }

    return pools.size();
  }

  public void saveMapPools() {
    mapPools.entrySet().stream()
        .forEach(
            e -> {
              String nextMap = null;
              if (e.getKey() instanceof Rotation) {
                nextMap = e.getKey().getNextMap().getName();
              }

              boolean active =
                  getActiveMapPool() != null
                      && getActiveMapPool().getName().equalsIgnoreCase(e.getKey().getName())
                      && e.getKey().isDynamic();
              e.getValue().update(nextMap, active);
            });
  }

  public MapPool getActiveMapPool() {
    return activeMapPool;
  }

  public List<MapPool> getMapPools() {
    return mapPools.keySet().stream().sorted().collect(Collectors.toList());
  }

  private void updateActiveMapPool(MapPool mapPool, Match match) {
    updateActiveMapPool(mapPool, match, false, null, null, 0);
  }

  public void updateActiveMapPool(
      MapPool mapPool,
      Match match,
      boolean force,
      @Nullable CommandSender sender,
      @Nullable Duration timeLimit,
      int matchLimit) {
    saveMapPools();

    if (mapPool == activeMapPool) return;

    activeMapPool.unloadPool(match);

    // Set new active pool
    activeMapPool = mapPool;
    matchCount = 0;

    if (!activeMapPool.isDynamic()) {
      poolStartTime = Instant.now();
      if (timeLimit != null) {
        poolTimeLimit = timeLimit;
      }
      matchCountLimit = Math.max(0, matchLimit);
    } else {
      poolStartTime = null;
      poolTimeLimit = null;
      matchCountLimit = 0;
    }

    // Call a MapPoolAdjustEvent so plugins can listen when map pool has changed
    match.callEvent(
        new MapPoolAdjustEvent(
            activeMapPool, mapPool, match, force, sender, poolTimeLimit, matchCountLimit));
  }

  /**
   * Method to be kept for the future, as it's very useful and could be used for a variety of
   * things.
   *
   * @param name The name of the desired map pool
   * @return The {@link MapPool} which matches the input name
   */
  public MapPool getMapPoolByName(String name) {
    return mapPools.keySet().stream()
        .filter(rot -> rot.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  protected MapInfo getOverriderMap() {
    return overriderMap;
  }

  public CustomVotingPoolOptions getCustomVoteOptions() {
    return options;
  }

  @Override
  public MapInfo popNextMap() {
    if (overriderMap != null) {
      MapInfo overrider = overriderMap;
      overriderMap = null;
      return overrider;
    }

    if (activeMapPool == null) {
      getActiveMapPool();
    }

    return activeMapPool.popNextMap();
  }

  @Override
  public MapInfo getNextMap() {
    if (overriderMap != null) return overriderMap;
    if (activeMapPool != null) return activeMapPool.getNextMap();
    return null;
  }

  @Override
  public void setNextMap(MapInfo map) {
    overriderMap = map;
    activeMapPool.setNextMap(map); // Notify pool a next map has been set
  }

  @Override
  public void resetNextMap() {
    if (overriderMap != null) {
      overriderMap = null;
    }
  }

  public Optional<MapPool> getAppropriateDynamicPool(Match match) {
    int obs =
        match.getModule(BlitzMatchModule.class) != null
            ? (int) (match.getObservers().size() * 0.85)
            : (int) (match.getObservers().size() * 0.5);
    int activePlayers = match.getPlayers().size() - obs;
    return mapPools.keySet().stream()
        .filter(pool -> pool.isDynamic())
        .filter(pool -> activePlayers >= pool.getPlayers())
        .max(MapPool::compareTo);
  }

  @Override
  public void matchEnded(Match match) {
    if (hasMatchCountLimit()) {
      matchCount++;
    }

    if (activeMapPool.isDynamic() || shouldRevert(match)) {
      getAppropriateDynamicPool(match).ifPresent(pool -> updateActiveMapPool(pool, match));
    }

    activeMapPool.matchEnded(match);
  }

  private boolean shouldRevert(Match match) {
    return !match.getPlayers().stream()
            .filter(mp -> mp.getBukkit().hasPermission(Permissions.STAFF))
            .findAny()
            .isPresent()
        || !activeMapPool.isDynamic()
            && poolTimeLimit != null
            && TimeUtils.isLongerThan(Duration.between(poolStartTime, Instant.now()), poolTimeLimit)
        || hasMatchCountLimit() && (matchCount > matchCountLimit);
  }

  private boolean hasMatchCountLimit() {
    return !activeMapPool.isDynamic() && (matchCountLimit > 0);
  }
}
