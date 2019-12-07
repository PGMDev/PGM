package tc.oc.pgm.rotation;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.commands.MapCommands;
import tc.oc.pgm.map.PGMMap;

public class Rotation {
  private ConfigurationSection configurationSection;

  private String name;
  private boolean enabled;
  private List<PGMMap> maps = new ArrayList<>();
  private int players;

  private int position = 0;
  private boolean overwritten = false;

  public Rotation(ConfigurationSection configurationSection, String name) {
    this.configurationSection = configurationSection;
    this.name = name;
  }

  public ConfigurationSection getConfigurationSection() {
    return configurationSection;
  }

  public void setConfigurationSection(ConfigurationSection configurationSection) {
    this.configurationSection = configurationSection;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<PGMMap> getMaps() {
    return maps;
  }

  public void setMaps(List<PGMMap> maps) {
    this.maps = maps;
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

  public boolean isOverwritten() {
    return overwritten;
  }

  public void setOverwritten(boolean overwritten) {
    this.overwritten = overwritten;
  }

  public int getPosition() {
    return position;
  }

  public void load() {
    configurationSection
        .getStringList("maps")
        .forEach(
            map -> {
              if (PGM.GLOBAL.get().getMapLibrary().getMapByNameOrId(map).isPresent()) {
                maps.add(PGM.GLOBAL.get().getMapLibrary().getMapByNameOrId(map).get());
              } else {
                PGM.GLOBAL
                    .get()
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

    setEnabled(configurationSection.getBoolean("enabled"));
    setPlayers(configurationSection.getInt("players"));
  }

  public void rotate() {
    if (!overwritten) {
      if (position + 1 == getMaps().size()) {
        position = 0;
      } else {
        position++;
      }
    } else overwritten = false;
  }

  public void overwriteWithMap(PGMMap map) {
    overwritten = true;
    MapCommands.setNextMap(map);
  }
}
