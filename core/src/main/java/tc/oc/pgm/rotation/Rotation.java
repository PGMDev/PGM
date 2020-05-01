package tc.oc.pgm.rotation;

import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;

public class Rotation extends MapPool {

  private int position;

  public Rotation(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);

    @Nullable MapInfo nextMap = PGM.get().getMapLibrary().getMap(manager.getNextMapForPool(name));
    if (nextMap != null) this.position = getMapPosition(nextMap);
    else {
      PGM.get()
          .getLogger()
          .log(
              Level.SEVERE, "Could not resolve next map from rotations, resuming initial position");
    }
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
