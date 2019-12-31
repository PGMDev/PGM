package tc.oc.pgm.rotation;

import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.map.PGMMap;

public class Rotation extends MapPool {

  private int position;

  public Rotation(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);

    Optional<PGMMap> nextMap =
        PGM.get().getMapLibrary().getMapByNameOrId(section.getString("next_map"));
    if (nextMap.isPresent()) this.position = getMapPosition(nextMap.get());
    else {
      PGM.get()
          .getLogger()
          .log(
              Level.SEVERE,
              "Could not resolve next map from rotations. Resuming on initial position: 0");
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

  private int getMapPosition(PGMMap map) {
    int count = 0;

    for (PGMMap pgmMap : maps) {
      if (pgmMap.getName().equals(map.getName())) break;
      count++;
    }

    return count;
  }

  private PGMMap getMapInPosition(int position) {
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
    configSection.set("next_map", getMapInPosition(position).getName());
    manager.saveMapPools();
  }

  @Override
  public PGMMap popNextMap() {
    PGMMap nextMap = getMapInPosition(position);
    advance();
    return nextMap;
  }

  @Override
  public PGMMap getNextMap() {
    return maps.get(position);
  }
}
