package tc.oc.pgm.server;

import net.minecraft.server.v1_8_R3.PropertyManager;
import org.bukkit.craftbukkit.libs.joptsimple.OptionSet;

/** A {@link PropertyManager} that also check for environment variables. */
public class EnvPropertyManager extends PropertyManager {

  public EnvPropertyManager(OptionSet options) {
    super(options);
  }

  @Override
  public String getString(String key, String val) {
    final String envVal = System.getenv("MINECRAFT_" + key.toUpperCase().replaceAll("-", "_"));
    if (envVal == null || envVal.isEmpty()) {
      return super.getString(key, val);
    }

    setProperty(key, envVal);
    return envVal;
  }
}
