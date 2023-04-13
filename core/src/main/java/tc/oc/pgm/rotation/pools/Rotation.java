package tc.oc.pgm.rotation.pools;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;

public class Rotation extends MapPool {

  private int position;

  public Rotation(
      MapPoolType type, String name, MapPoolManager manager, ConfigurationSection section) {
    super(type, name, manager, section);

    @Nullable MapInfo nextMap = PGM.get().getMapLibrary().getMap(manager.getNextMapForPool(name));
    if (nextMap != null) this.position = getMapPosition(nextMap);
    else {
      PGM.get()
          .getLogger()
          .log(
              Level.SEVERE, "Could not resolve next map from rotations, resuming initial position");
    }
  }

  public Rotation(
      MapPoolType type,
      MapPoolManager manager,
      String name,
      boolean enabled,
      int players,
      boolean dynamic,
      Duration cycleTime,
      List<MapInfo> maps) {
    super(type, name, manager, enabled, players, dynamic, cycleTime, maps);
  }

  public void setPosition(int position) {
    this.position = position % maps.size();
  }

  public int getPosition() {
    return position;
  }

  public int getNextPosition() {
    return (position + 1) % maps.size();
  }

  private int getMapPosition(MapInfo map) {
    for (int i = 0; i < maps.size(); i++) {
      if (maps.get(i).getName().equals(map.getName())) {
        return i;
      }
    }

    PGM.get()
        .getLogger()
        .log(
            Level.SEVERE,
            "Could not resolve next map from rotations. Resuming on initial position: 0");
    return 0;
  }

  private MapInfo getMapInPosition(int position) {
    if (position < 0 || position >= maps.size()) {
      PGM.get()
          .getLogger()
          .log(
              Level.WARNING,
              "An unexpected call to map in position "
                  + position
                  + " from rotation with size "
                  + maps.size()
                  + " has been issued. Returning map in position 0 instead.");
      return maps.get(0);
    }

    return maps.get(position);
  }

  private void advance() {
    advance(1);
  }

  public void advance(int positions) {
    position = (position + positions) % maps.size();
    manager.saveMapPools();
  }

  @Override
  public MapInfo popNextMap() {
    MapInfo nextMap = getMapInPosition(position);
    advance();
    return nextMap;
  }

  @Override
  public MapInfo getNextMap() {
    return maps.get(position);
  }
}
