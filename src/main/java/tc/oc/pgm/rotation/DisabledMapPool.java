package tc.oc.pgm.rotation;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.map.PGMMap;

public class DisabledMapPool extends MapPool {
  DisabledMapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public PGMMap popNextMap() {
    return getRandom();
  }

  @Override
  public PGMMap getNextMap() {
    return null;
  }
}
