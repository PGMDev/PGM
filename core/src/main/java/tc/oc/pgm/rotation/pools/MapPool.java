package tc.oc.pgm.rotation.pools;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.MapPoolManager;

/** Rotation of maps, a type of {@link MapOrder} */
public abstract class MapPool implements MapOrder, Comparable<MapPool> {
  protected final MapPoolManager manager;

  @Getter
  protected final MapPoolType type;

  @Getter
  protected final String name;

  @Getter
  protected final boolean enabled;

  @Getter
  protected final List<MapInfo> maps;

  @Getter
  protected final int players;

  @Getter
  protected final Duration cycleTime;

  @Getter
  protected final boolean dynamic;

  MapPool(
      MapPoolType type,
      String name,
      MapPoolManager manager,
      ConfigurationSection section,
      MapParser maps) {
    this(
        type,
        name,
        manager,
        section.getBoolean("enabled"),
        section.getInt("players"),
        section.getBoolean("dynamic", true),
        parseDuration(section.getString("cycle-time", "-1s")),
        maps.getMaps());
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
    this.maps = Collections.unmodifiableList(maps);
  }

  protected MapInfo getRandom() {
    return maps.get((int) (Math.random() * maps.size()));
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
  public void unloadPool(Match match) {}

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
