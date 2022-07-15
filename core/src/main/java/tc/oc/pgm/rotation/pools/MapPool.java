package tc.oc.pgm.rotation.pools;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.MapPoolManager;

/** Rotation of maps, a type of {@link MapOrder} */
public abstract class MapPool implements MapOrder, Comparable<MapPool> {
  protected final MapPoolManager manager;
  protected final ConfigurationSection configSection;

  protected final String name;
  protected final boolean enabled;
  protected final List<MapInfo> maps;
  protected final int players;
  protected final Duration cycleTime;

  protected final boolean dynamic;

  public static MapPool of(MapPoolManager manager, FileConfiguration config, String key) {
    ConfigurationSection section = config.getConfigurationSection("pools." + key);
    String type = section.getString("type").toLowerCase();
    switch (type) {
      case "ordered":
        return new Rotation(manager, section, key);
      case "voted":
        return new VotingPool(manager, section, key);
      case "shuffled":
        return new RandomMapPool(manager, section, key);
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
    this.dynamic = section.getBoolean("dynamic", true);
    this.cycleTime = parseDuration(section.getString("cycle-time", "-1s"));

    MapLibrary library = PGM.get().getMapLibrary();
    List<MapInfo> mapList =
        section.getStringList("maps").stream()
            .map(mapName -> getMap(library, mapName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    this.maps = Collections.unmodifiableList(mapList);
  }

  private MapInfo getMap(MapLibrary library, String mapName) {
    @Nullable MapInfo map = library.getMap(mapName);
    if (map != null) return map;
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

  public boolean isDynamic() {
    return dynamic;
  }

  public List<MapInfo> getMaps() {
    return maps;
  }

  public int getPlayers() {
    return players;
  }

  protected MapInfo getRandom() {
    return maps.get((int) (Math.random() * maps.size()));
  }

  @Override
  public Duration getCycleTime() {
    return cycleTime;
  }

  /**
   * Override as no-op set next, as the {@link MapPoolManager} is the one responsible for it.
   *
   * @param map The map to set next
   */
  @Override
  public void setNextMap(MapInfo map) {}

  /**
   * Called when this map pool is going to be switched out
   *
   * @param match The match that is currently ending
   */
  public void unloadPool(Match match) {};

  @Override
  public int compareTo(MapPool o) {
    if (!o.isDynamic()) {
      return -1;
    } else if (!isDynamic()) {
      return 1;
    } else {
      return Integer.compare(players, o.players);
    }
  }
}
