package tc.oc.pgm.rotation;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;

public class DisabledMapPool extends MapPool {
  DisabledMapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public MapInfo popNextMap() {
    return getRandom();
  }

  @Override
  public MapInfo getNextMap() {
    return null;
  }
}
