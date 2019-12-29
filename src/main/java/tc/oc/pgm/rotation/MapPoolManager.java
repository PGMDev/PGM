package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.PGMMap;

/**
 * Manages all the existing {@link MapPool}s, as for maintaining their order, and updating the one
 * {@link PGM} will use after every match depending on the player count (Dynamic Rotations)
 */
public class MapPoolManager implements PGMMapOrder {

  private Logger logger;

  private File mapPoolsFile;
  private FileConfiguration mapPoolFileConfig;
  private List<MapPool> mapPools = new ArrayList<>();
  private MapPool activeMapPool;
  /** When a {@link PGMMap} is manually set next, it overrides the rotation order * */
  private PGMMap overriderMap;

  public MapPoolManager(Logger logger, File mapPoolsFile) {
    this.mapPoolsFile = mapPoolsFile;
    this.logger = logger;

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

  private void loadMapPools() {
    mapPools =
        mapPoolFileConfig.getConfigurationSection("pools").getKeys(false).stream()
            .map(key -> MapPool.of(this, mapPoolFileConfig, key))
            .filter(MapPool::isEnabled)
            .collect(Collectors.toList());

    MapPool lastActiveMapPool = getMapPoolByName(mapPoolFileConfig.getString("last_active"));
    setActiveMapPool(lastActiveMapPool);
  }

  public void saveMapPools() {
    try {
      mapPoolFileConfig.save(mapPoolsFile);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save next map for future reference", e);
    }
  }

  private void setActiveMapPool(MapPool activeMapPool) {
    this.activeMapPool = activeMapPool;
  }

  public MapPool getActiveMapPool() {
    return activeMapPool;
  }

  public List<MapPool> getMapPools() {
    return mapPools;
  }

  private void updateActiveMapPool(MapPool mapPool) {
    if (mapPool == activeMapPool) return;

    setActiveMapPool(mapPool);

    mapPoolFileConfig.set("last_active", activeMapPool.getName());
    saveMapPools();

    Bukkit.broadcastMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "rotations.rotationChange",
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

  protected PGMMap getOverriderMap() {
    return overriderMap;
  }

  @Override
  public PGMMap popNextMap() {
    if (overriderMap != null) {
      PGMMap overrider = overriderMap;
      overriderMap = null;
      return overrider;
    }
    return activeMapPool.popNextMap();
  }

  @Override
  public PGMMap getNextMap() {
    if (overriderMap != null) return overriderMap;
    if (activeMapPool != null) return activeMapPool.getNextMap();
    return null;
  }

  @Override
  public void setNextMap(PGMMap map) {
    overriderMap = map;
    activeMapPool.setNextMap(map); // Notify pool a next map has been set
  }

  @Override
  public void matchEnded(Match match) {
    int activePlayers = match.getPlayers().size() - (match.getObservers().size() / 2);

    mapPools.stream()
        .filter(rot -> activePlayers >= rot.getPlayers())
        .max(MapPool::compareTo)
        .ifPresent(this::updateActiveMapPool);

    activeMapPool.matchEnded(match);
  }
}
