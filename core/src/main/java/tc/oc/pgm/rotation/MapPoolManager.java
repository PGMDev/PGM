package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.Config;
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

  private FileConfiguration mapPoolFileConfig;
  private List<MapPool> mapPools = new ArrayList<>();
  private MapPool activeMapPool;

  /* If a time limit is added via /setpool <name> -t [time], then after this duration the map pool will revert automaticly */
  private Duration poolTimeLimit = null;
  private Instant poolStartTime = null;

  /** When a {@link MapInfo} is manually set next, it overrides the rotation order * */
  private MapInfo overriderMap;

  private Datastore database;

  public MapPoolManager(Logger logger, File mapPoolsFile, Datastore database) {
    this.logger = logger;
    this.database = database;

    if (!mapPoolsFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(PGM.get().getResource("map-pools.yml"), mapPoolsFile);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create the map-pools.yml file", e);
        return;
      }
    }

    this.mapPoolFileConfig = YamlConfiguration.loadConfiguration(mapPoolsFile);
    loadMapPools();
  }

  public @Nullable String getNextMapForPool(String poolName) {
    return database.getMapActivity(poolName).getMapName();
  }

  private void loadMapPools() {
    mapPools =
        mapPoolFileConfig.getConfigurationSection("pools").getKeys(false).stream()
            .map(key -> MapPool.of(this, mapPoolFileConfig, key))
            .filter(MapPool::isEnabled)
            .collect(Collectors.toList());

    mapPools.stream()
        .forEach(
            pool -> {
              MapActivity ma = database.getMapActivity(pool.getName());
              if (ma.isActive() && !pool.isDynamic()) {
                activeMapPool = pool;
                logger.log(Level.INFO, "Resuming last active map pool (" + pool.getName() + ")");
              }
            });

    if (activeMapPool == null) {
      logger.log(Level.WARNING, "No active map pool was found, defaulting to first dynamic pool.");
      activeMapPool = mapPools.stream().filter(mp -> mp.isDynamic()).findFirst().orElse(null);

      if (activeMapPool == null) {
        logger.log(Level.SEVERE, "Failed to find any dynamic map pool!");
      }
    }
  }

  public void saveMapPools() {
    mapPools.stream()
        .forEach(
            pool -> {
              String nextMap = null;
              if (pool instanceof Rotation) {
                nextMap = pool.getNextMap().getName();
              }
              boolean active =
                  activeMapPool.getName().equalsIgnoreCase(pool.getName())
                      && activeMapPool.isDynamic();
              database.getMapActivity(pool.getName()).update(nextMap, active);
            });
  }

  public MapPool getActiveMapPool() {
    return activeMapPool;
  }

  public List<MapPool> getMapPools() {
    return mapPools;
  }

  private void updateActiveMapPool(MapPool mapPool, Match match) {
    updateActiveMapPool(mapPool, match, false, null, null);
  }

  public void updateActiveMapPool(
      MapPool mapPool,
      Match match,
      boolean force,
      @Nullable CommandSender sender,
      @Nullable Duration timeLimit) {
    saveMapPools();

    if (mapPool == activeMapPool) return;

    activeMapPool.unloadPool(match);

    // Set new active pool
    activeMapPool = mapPool;

    if (!activeMapPool.isDynamic()) {
      poolStartTime = Instant.now();
      if (timeLimit != null) {
        poolTimeLimit = timeLimit;
      }
    } else {
      poolStartTime = null;
      poolTimeLimit = null;
    }

    // Call a MapPoolAdjustEvent so plugins can listen when map pool has changed
    match.callEvent(
        new MapPoolAdjustEvent(activeMapPool, mapPool, match, force, sender, poolTimeLimit));
  }

  /**
   * Method to be kept for the future, as it's very useful and could be used for a variety of
   * things.
   *
   * @param name The name of the desired map pool
   * @return The {@link MapPool} which matches the input name
   */
  public MapPool getMapPoolByName(String name) {
    return mapPools.stream()
        .filter(rot -> rot.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  protected MapInfo getOverriderMap() {
    return overriderMap;
  }

  @Override
  public MapInfo popNextMap() {
    if (overriderMap != null) {
      MapInfo overrider = overriderMap;
      overriderMap = null;
      return overrider;
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

  public Optional<MapPool> getAppropriateDynamicPool(Match match) {
    int obs =
        match.getModule(BlitzMatchModule.class) != null
            ? (int) (match.getObservers().size() * 0.85)
            : (int) (match.getObservers().size() * 0.5);
    int activePlayers = match.getPlayers().size() - obs;
    return mapPools.stream()
        .filter(rot -> activePlayers >= rot.getPlayers())
        .max(MapPool::compareTo);
  }

  @Override
  public void matchEnded(Match match) {
    int activePlayers = match.getPlayers().size() - (match.getObservers().size() / 2);

    mapPools.stream()
        .filter(rot -> activePlayers >= rot.getPlayers())
        .max(MapPool::compareTo)
        .ifPresent(pool -> updateActiveMapPool(pool, match));

    activeMapPool.matchEnded(match);
  }

  private boolean shouldRevert(Match match) {
    return (Config.MapPools.areStaffRequired()
            && !match.getPlayers().stream()
                .filter(mp -> mp.getBukkit().hasPermission(Permissions.STAFF))
                .findAny()
                .isPresent())
        || !activeMapPool.isDynamic()
            && poolTimeLimit != null
            && TimeUtils.isLongerThan(
                Duration.between(poolStartTime, Instant.now()), poolTimeLimit);
  }
}
