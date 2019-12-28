package tc.oc.pgm.rotation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;

/** Rotation of maps, a type of {@link PGMMapOrder} */
public class Rotation implements PGMMapOrder, Comparable<Rotation> {
  private final RotationManager manager;
  private final ConfigurationSection configSection;

  private final String name;
  private final boolean enabled;
  private final List<PGMMap> maps;
  private final int players;

  private int position;

  Rotation(RotationManager manager, ConfigurationSection configSection, String name) {
    this.manager = manager;
    this.configSection = configSection;
    this.name = name;
    this.enabled = configSection.getBoolean("enabled");
    this.players = configSection.getInt("players");

    MapLibrary library = PGM.get().getMapLibrary();
    List<PGMMap> mapList =
        configSection.getStringList("maps").stream()
            .map(
                mapName -> {
                  Optional<PGMMap> optMap = library.getMapByNameOrId(mapName);
                  if (optMap.isPresent()) return optMap.get();
                  PGM.get()
                      .getLogger()
                      .warning(
                          "[Rotation] ["
                              + name
                              + "] "
                              + mapName
                              + " not found in map repo. Ignoring...");
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    this.maps = Collections.unmodifiableList(mapList);

    Optional<PGMMap> nextMap = library.getMapByNameOrId(configSection.getString("next_map"));
    if (nextMap.isPresent()) this.position = getMapPosition(nextMap.get());
    else {
      PGM.get()
          .getLogger()
          .log(
              Level.SEVERE,
              "Could not resolve next map from rotations. Resuming on initial position: 0");
    }
  }

  public String getName() {
    return name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public List<PGMMap> getMaps() {
    return Collections.unmodifiableList(maps);
  }

  public int getPlayers() {
    return players;
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
    manager.saveRotations();
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

  @Override
  public void setNextMap(PGMMap map) {}

  @Override
  public int compareTo(Rotation o) {
    return Integer.compare(this.players, o.players);
  }
}
