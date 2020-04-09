package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.PoolActivity;
import tc.oc.pgm.api.match.Match;
import tc.oc.util.bukkit.translations.AllTranslations;

/**
 * Manages all the existing {@link MapPool}s, as for maintaining their order, and updating the one
 * {@link PGM} will use after every match depending on the player count (Dynamic Rotations)
 */
public class MapPoolManager implements MapOrder {

  private Logger logger;

  private FileConfiguration mapPoolFileConfig;
  private List<MapPool> mapPools = new ArrayList<>();
  private MapPool activeMapPool;
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
    return database.getPoolActivity(poolName).getNextMap();
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
              PoolActivity pa = database.getPoolActivity(pool.getName());
              if (pa.isLastActive()) {
                activeMapPool = pool;
                logger.log(Level.INFO, "Resuming last active map pool (" + pool.getName() + ")");
              }
            });

    if (activeMapPool == null) {
      logger.log(Level.WARNING, "No active map pool was found, defaulting to first defined pool.");
      if (mapPools.get(0) != null) {
        activeMapPool = mapPools.get(0);
      } else {
        logger.log(Level.SEVERE, "Failed to find any defined map pool!");
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
              boolean active = activeMapPool.getName().equalsIgnoreCase(pool.getName());
              database.getPoolActivity(pool.getName()).updatePool(nextMap, active);
            });
  }

  public MapPool getActiveMapPool() {
    return activeMapPool;
  }

  public List<MapPool> getMapPools() {
    return mapPools;
  }

  private void updateActiveMapPool(MapPool mapPool, Match match) {
    saveMapPools();

    if (mapPool == activeMapPool) return;

    activeMapPool.unloadPool(match);
    activeMapPool = mapPool;

    match.sendMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "pools.poolChange",
                    Bukkit.getConsoleSender(),
                    (ChatColor.AQUA + mapPool.getName() + ChatColor.GREEN)));
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

  @Override
  public void matchEnded(Match match) {
    int activePlayers = match.getPlayers().size() - (match.getObservers().size() / 2);

    mapPools.stream()
        .filter(rot -> activePlayers >= rot.getPlayers())
        .max(MapPool::compareTo)
        .ifPresent(pool -> updateActiveMapPool(pool, match));

    activeMapPool.matchEnded(match);
  }
}
