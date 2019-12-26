package tc.oc.pgm.rotation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;

/** Rotation of maps, a type of {@link PGMMapOrder} */
public abstract class MapPool implements PGMMapOrder, Comparable<MapPool> {
  protected final MapPoolManager manager;
  protected final ConfigurationSection configSection;

  protected final String name;
  protected final boolean enabled;
  protected final List<PGMMap> maps;
  protected final int players;

  public static MapPool of(MapPoolManager manager, FileConfiguration config, String key) {
    ConfigurationSection section = config.getConfigurationSection("pools." + key);
    String type = section.getString("type").toLowerCase();
    switch (type) {
      case "ordered":
        return new Rotation(manager, section, key);
      case "voted":
        return new VotingPool(manager, section, key);
      default:
        PGM.get().getLogger().severe("Invalid map pool type for " + key + ": '" + type + "'");
        return new DisabledMapPool(manager, section, key);
    }
  }

  MapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    this.manager = manager;
    this.configSection = section;
    this.name = name;
    this.enabled = section.getBoolean("enabled");
    this.players = section.getInt("players");

    MapLibrary library = PGM.get().getMapLibrary();
    List<PGMMap> mapList =
        section.getStringList("maps").stream()
            .map(mapName -> getMap(library, mapName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    this.maps = Collections.unmodifiableList(mapList);
  }

  private PGMMap getMap(MapLibrary library, String mapName) {
    Optional<PGMMap> optMap = library.getMapByNameOrId(mapName);
    if (optMap.isPresent()) return optMap.get();
    PGM.get()
        .getLogger()
        .warning("[MapPool] [" + name + "] " + mapName + " not found in map repo. Ignoring...");
    return null;
  }

  public String getName() {
    return name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public List<PGMMap> getMaps() {
    return maps;
  }

  public int getPlayers() {
    return players;
  }

  protected PGMMap getRandom() {
    return maps.get((int) (Math.random() * maps.size()));
  }

  /**
   * Override as no-op set next, as the {@link MapPoolManager} is the one responsible for it.
   *
   * @param map The map to set next
   */
  @Override
  public void setNextMap(PGMMap map) {}

  @Override
  public int compareTo(MapPool o) {
    return Integer.compare(players, o.players);
  }
}
