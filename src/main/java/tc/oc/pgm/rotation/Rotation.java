package tc.oc.pgm.rotation;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.map.PGMMap;

public class Rotation implements PGMMapOrderProvider {
  private ConfigurationSection configurationSection;

  private String name;
  private boolean enabled;
  private List<PGMMap> maps = new ArrayList<>();
  private int players;
  private int position;

  Rotation(ConfigurationSection configurationSection, String name) {
    this.configurationSection = configurationSection;
    this.name = name;
    this.enabled = configurationSection.getBoolean("enabled");
    this.players = configurationSection.getInt("players");
    this.position = configurationSection.getInt("position");
  }

  public String getName() {
    return name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<PGMMap> getMaps() {
    return Collections.unmodifiableList(maps);
  }

  public int getPlayers() {
    return players;
  }

  public void setPlayers(int players) {
    this.players = players;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  public void load() {
    configurationSection
        .getStringList("maps")
        .forEach(
            map -> {
              final Optional<PGMMap> mapByNameOrId =
                  PGM.get().getMapLibrary().getMapByNameOrId(map);
              if (mapByNameOrId.isPresent()) maps.add(mapByNameOrId.get());
              else {
                PGM.get()
                    .getLogger()
                    .warning(
                        "[Rotation] "
                            + "["
                            + name
                            + "] "
                            + map
                            + " not found in map repo. Ignoring...");
              }
            });
  }

  private void rotate() {
    if (position + 1 == maps.size() || position + 1 > maps.size()) {
      position = 0;
    } else {
      movePosition(1);
    }
  }

  public void movePosition(int positions) {
    if (((position + positions) + 1) > maps.size()) {
      position = ((position + positions) + 1) % maps.size();
    } else {
      position = position + positions;
    }
  }

  public PGMMap getMapInPosition(int position) {
    return maps.get(position);
  }

  @Override
  public PGMMap popNextMap() {
    PGMMap nextMap = maps.get(position);
    rotate();
    return nextMap;
  }

  @Override
  public PGMMap getNextMap() {
    return maps.get(position);
  }

  @Override
  public PGMMap popFallbackMap() {
    return null;
  }

  @Override
  public void setNextMap(PGMMap map) {}
}
