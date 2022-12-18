package tc.oc.pgm.rotation.pools;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.MapPoolManager;

/** Rotation of maps, a type of {@link MapOrder} */
public abstract class MapPool implements MapOrder, Comparable<MapPool> {
  protected final MapPoolManager manager;

  protected final MapPoolType type;
  protected final String name;
  protected final boolean enabled;
  protected final List<MapInfo> maps;
  protected final int players;
  protected final Duration cycleTime;

  protected final boolean dynamic;

  MapPool(MapPoolType type, String name, MapPoolManager manager, ConfigurationSection section) {
    this(
        type,
        name,
        manager,
        section.getBoolean("enabled"),
        section.getInt("players"),
        section.getBoolean("dynamic", true),
        parseDuration(section.getString("cycle-time", "-1s")),
        buildMapList(section.getStringList("maps"), name));
  }

  MapPool(
      MapPoolType type,
      String name,
      MapPoolManager manager,
      boolean enabled,
      int players,
      boolean dynamic,
      Duration cycleTime,
      List<MapInfo> maps) {
    this.type = type;
    this.name = name;
    this.manager = manager;
    this.enabled = enabled;
    this.players = players;
    this.dynamic = dynamic;
    this.cycleTime = cycleTime;
    this.maps = maps;
  }

  private static List<MapInfo> buildMapList(List<String> mapNames, String poolName) {
    if (mapNames == null) return new ArrayList<>();

    List<MapInfo> mapList = new ArrayList<>(mapNames.size());

    for (String mapName : mapNames) {
      MapInfo map = PGM.get().getMapLibrary().getMap(mapName);
      if (map != null) {
        mapList.add(map);
      } else {
        PGM.get()
            .getLogger()
            .warning(
                "[MapPool] [" + poolName + "] " + mapName + " not found in map repo. Ignoring...");
      }
    }

    return Collections.unmodifiableList(mapList);
  }

  public MapPoolType getType() {
    return type;
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
