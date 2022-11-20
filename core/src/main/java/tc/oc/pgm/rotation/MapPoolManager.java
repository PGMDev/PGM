package tc.oc.pgm.rotation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.events.MapPoolAdjustEvent;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.VotePoolOptions;
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
  private MapOrder fallback; // Fallback map order in case no pool exists

  /* If a time limit is added via /setpool <name> -t [time], then after this duration the map pool will revert automatically */
  private Duration poolTimeLimit = null;
  private Instant poolStartTime = null;

  private int matchCountLimit = 0; // The limit for when to revert to dynamic pools
  private int matchCount = 0; // # of completed matches since start of pool

  /** When a {@link MapInfo} is manually set next, it overrides the rotation order * */
  private MapInfo overriderMap;

  /** Options related to voting pools, allows for custom voting @see {@link VotingPool} * */
  private final VotePoolOptions options;

  private final Datastore database;

  public MapPoolManager(Logger logger, File mapPoolsFile, Datastore database) {
    this.logger = logger;
    this.mapPoolsFile = mapPoolsFile;
    this.database = database;
    this.options = new VotePoolOptions();

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

  private void loadMapPools() {
    this.mapPools.clear(); // For reloads

    ConfigurationSection pools = mapPoolFileConfig.getConfigurationSection("pools");
    if (pools != null && pools.getKeys(false) != null && !pools.getKeys(false).isEmpty()) {
      pools.getKeys(false).stream()
          .map(key -> MapPoolType.buildPool(this, mapPoolFileConfig, key))
          .filter(MapPool::isEnabled)
          .forEach(pool -> mapPools.put(pool, database.getMapActivity(pool.getName())));

      activeMapPool =
          mapPools.entrySet().stream()
              .filter(e -> e.getValue().isActive())
              .findFirst()
              .map(Map.Entry::getKey)
              .orElse(null);
    }

    if (activeMapPool == null) {
      logger.log(Level.WARNING, "No active map pool was found, defaulting to first dynamic pool.");
      activeMapPool =
          mapPools.keySet().stream().sorted().filter(MapPool::isDynamic).findFirst().orElse(null);
      if (activeMapPool == null) {
        logger.log(
            Level.SEVERE,
            "Failed to find any dynamic map pool! Will use fallback map order (shuffled)");
      }
    } else {
      logger.log(Level.INFO, "Resuming last active map pool (" + activeMapPool.getName() + ")");
    }
  }

  public void saveMapPools() {
    mapPools.forEach(
        (key, value) -> {
          String nextMap = null;
          if (key instanceof Rotation) {
            nextMap = key.getNextMap().getName();
          }

          boolean active =
              getActiveMapPool() != null
                  && getActiveMapPool().getName().equalsIgnoreCase(key.getName())
                  && key.isDynamic();
          value.update(nextMap, active);
        });
  }

  public MapPool getActiveMapPool() {
    return activeMapPool;
  }

  public List<MapPool> getMapPools() {
    return mapPools.keySet().stream().sorted().collect(Collectors.toList());
  }

  public Stream<MapPool> getMapPoolStream() {
    return mapPools.keySet().stream();
  }

  public int getPoolSize() {
    return mapPools.size();
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

    if (activeMapPool != null) {
      activeMapPool.unloadPool(match);
    }

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

  public MapInfo getOverriderMap() {
    return overriderMap;
  }

  public VotePoolOptions getVoteOptions() {
    return options;
  }

  public MapOrder getFallback() {
    if (fallback == null)
      fallback = new RandomMapOrder(Lists.newArrayList(PGM.get().getMapLibrary().getMaps()));
    return fallback;
  }

  @Override
  public MapInfo popNextMap() {
    if (overriderMap != null) {
      MapInfo overrider = overriderMap;
      overriderMap = null;
      return overrider;
    }

    if (activeMapPool == null) {
      return getFallback().popNextMap();
    }

    return activeMapPool.popNextMap();
  }

  @Override
  public MapInfo getNextMap() {
    if (overriderMap != null) return overriderMap;
    return getOrder().getNextMap();
  }

  @Override
  public void setNextMap(MapInfo map) {
    overriderMap = map;
    // Notify pool/fallback a next map has been set
    getOrder().setNextMap(map);
  }

  public double getActivePlayers(Match match) {
    if (match == null) {
      Iterator<Match> matches = PGM.get().getMatchManager().getMatches();
      // Fallback to just raw online playercount
      if (!matches.hasNext()) return Bukkit.getOnlinePlayers().size();
      match = matches.next();
    }
    double obsBias = match.getModule(BlitzMatchModule.class) != null ? 0.85 : 0.5;
    return match.getParticipants().size() + match.getObservers().size() * obsBias;
  }

  public Optional<MapPool> getAppropriateDynamicPool(Match match) {
    double activePlayers = getActivePlayers(match);
    return mapPools.keySet().stream()
        .filter(MapPool::isDynamic)
        .filter(pool -> activePlayers >= pool.getPlayers())
        .max(MapPool::compareTo);
  }

  @Override
  public void matchEnded(Match match) {
    if (hasMatchCountLimit()) {
      matchCount++;
    }

    if (activeMapPool == null) {
      getFallback().matchEnded(match);
      return;
    }

    if (activeMapPool.isDynamic() || shouldRevert(match)) {
      getAppropriateDynamicPool(match).ifPresent(pool -> updateActiveMapPool(pool, match));
    }

    activeMapPool.matchEnded(match);
  }

  @Override
  public Duration getCycleTime() {
    Duration cycleTime;
    if (activeMapPool != null && !(cycleTime = activeMapPool.getCycleTime()).isNegative()) {
      return cycleTime;
    }
    return PGM.get().getConfiguration().getCycleTime();
  }

  private boolean shouldRevert(Match match) {
    return match.getPlayers().stream()
            .noneMatch(mp -> mp.getBukkit().hasPermission(Permissions.STAFF))
        || !activeMapPool.isDynamic()
            && poolTimeLimit != null
            && TimeUtils.isLongerThan(Duration.between(poolStartTime, Instant.now()), poolTimeLimit)
        || hasMatchCountLimit() && (matchCount > matchCountLimit);
  }

  private boolean hasMatchCountLimit() {
    return !activeMapPool.isDynamic() && (matchCountLimit > 0);
  }

  private MapOrder getOrder() {
    return activeMapPool != null ? activeMapPool : getFallback();
  }
}
