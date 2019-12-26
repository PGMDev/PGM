package tc.oc.pgm.rotation;

import org.bukkit.configuration.ConfigurationSection;

public class DisabledMapPool extends MapPool {
  public DisabledMapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
