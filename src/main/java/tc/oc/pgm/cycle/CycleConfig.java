package tc.oc.pgm.cycle;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.joda.time.Duration;
import tc.oc.server.ConfigUtils;

public class CycleConfig {
  public static final Duration DEFAULT_COUNTDOWN = Duration.standardSeconds(15);

  private final ConfigurationSection config;

  public CycleConfig(Configuration config) {
    this(config.getConfigurationSection("cycle"));
  }

  public CycleConfig(ConfigurationSection config) {
    this.config = config;
  }

  public Duration countdown() {
    return ConfigUtils.getDuration(config, "countdown", DEFAULT_COUNTDOWN);
  }

  public boolean runningMatch() {
    return config.getBoolean("running-match", false);
  }

  public Auto matchEmpty() {
    return new Auto("match-empty");
  }

  public Auto matchEnd() {
    return new Auto("match-end");
  }

  public class Auto {
    private final String key;

    public Auto(String key) {
      this.key = key;
    }

    ConfigurationSection getConfig() {
      return config.getConfigurationSection(key);
    }

    public boolean enabled() {
      return getConfig().getBoolean("enabled", false);
    }

    public Duration countdown() {
      return ConfigUtils.getDuration(getConfig(), "countdown", DEFAULT_COUNTDOWN);
    }
  }
}
