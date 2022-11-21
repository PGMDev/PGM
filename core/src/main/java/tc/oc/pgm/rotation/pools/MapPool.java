package tc.oc.pgm.rotation.pools;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.MapPoolManager;

/** Rotation of maps, a type of {@link MapOrder} */
public abstract class MapPool implements MapOrder, Comparable<MapPool> {
  protected final MapPoolManager manager;

  protected final MapPoolType type;
  protected final String identifier;
  protected final String name;
  protected final boolean enabled;
  protected final List<MapInfo> maps;
  protected final int players;
  protected final Duration cycleTime;

  protected final boolean dynamic;

  MapPool(
      MapPoolType type, MapPoolManager manager, ConfigurationSection section, String identifier) {
    this(
        type,
        manager,
        identifier,
        section.getString("display-name", identifier),
        section.getBoolean("enabled"),
        section.getInt("players"),
        section.getBoolean("dynamic", true),
        parseDuration(section.getString("cycle-time", "-1s")),
        parseMapList(identifier, section));
  }

  MapPool(
      MapPoolType type,
      MapPoolManager manager,
      String identifier,
      String name,
      boolean enabled,
      int players,
      boolean dynamic,
      Duration cycleTime,
      List<MapInfo> mapList) {
    this.type = type;
    this.manager = manager;
    this.identifier = identifier;
    this.name = name;
    this.enabled = enabled;
    this.players = players;
    this.dynamic = dynamic;
    this.cycleTime = cycleTime;
    this.maps = mapList;
  }

  public static List<MapInfo> parseMapList(String identifier, ConfigurationSection section) {
    MapLibrary library = PGM.get().getMapLibrary();
    List<MapInfo> mapList =
        section.getStringList("maps").stream()
            .map(mapName -> getMap(identifier, library, mapName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return Collections.unmodifiableList(mapList);
  }

  private static MapInfo getMap(String poolName, MapLibrary library, String mapName) {
    @Nullable MapInfo map = library.getMap(mapName);
    if (map != null) return map;
    PGM.get()
        .getLogger()
        .warning("[MapPool] [" + poolName + "] " + mapName + " not found in map repo. Ignoring...");
    return null;
  }

  public MapPoolType getType() {
    return type;
  }

  public String getIdentifier() {
    return identifier;
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
