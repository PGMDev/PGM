package tc.oc.pgm.start;

import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.util.bukkit.ConfigUtils;

public class StartConfig {
  private final ConfigurationSection config;

  public StartConfig(Configuration config) {
    this(config.getConfigurationSection("start"));
  }

  public StartConfig(ConfigurationSection config) {
    this.config = config;
  }

  public boolean autoStart() {
    return config.getBoolean("auto", true);
  }

  public Duration countdown() {
    return ConfigUtils.getDuration(config, "countdown", Duration.ofSeconds(30));
  }

  public Duration huddle() {
    return ConfigUtils.getDuration(config, "huddle", Duration.ZERO);
  }

  public @Nullable Duration timeout() {
    Duration d = ConfigUtils.getDuration(config, "timeout");
    return Duration.ZERO.equals(d) ? null : d;
  }
}
