package tc.oc.pgm.rotation.pools;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.VariableDuration;
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
  protected final VariableDuration cycleTime;

  protected final boolean dynamic;

  MapPool(
      MapPoolType type,
      String name,
      MapPoolManager manager,
      ConfigurationSection section,
      MapParser maps) {
    this.type = type;
    this.name = name;
    this.manager = manager;
    this.enabled = section.getBoolean("enabled");
    this.players = section.getInt("players");
    this.dynamic = section.getBoolean("dynamic", true);
    this.cycleTime = VariableDuration.parse(section.get("cycle-time"), "-1s");
    this.maps = Collections.unmodifiableList(maps.getMaps());
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
    this.cycleTime = VariableDuration.constant(cycleTime);
    this.maps = Collections.unmodifiableList(maps);
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
    return cycleTime.getDefault();
  }

  @Override
  public Duration getCycleTime(Match match) {
    return cycleTime.getDuration(match);
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
