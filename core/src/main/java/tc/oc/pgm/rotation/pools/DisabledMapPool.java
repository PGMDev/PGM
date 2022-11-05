package tc.oc.pgm.rotation.pools;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;

public class DisabledMapPool extends MapPool {
  DisabledMapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(null, name, manager, section);
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
