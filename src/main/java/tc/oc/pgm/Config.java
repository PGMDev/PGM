package tc.oc.pgm;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.joda.time.Duration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.events.ConfigLoadEvent;

public class Config {
  public static Configuration getConfiguration() {
    PGM pgm = PGM.get();
    if (pgm != null) {
      return pgm.getConfig();
    } else {
      return new YamlConfiguration();
    }
  }

  public static class AutoReload {
    public static boolean enabled() {
      return getConfiguration().getBoolean("map.auto-reload.enabled", true);
    }

    public static boolean reloadWhenError() {
      return getConfiguration().getBoolean("map.auto-reload.reload-when-error", false);
    }
  }

  public static class Rotations {
    public static boolean areEnabled() {
      return getConfiguration().getBoolean("rotations.enabled");
    }

    public static String getPath() {
      return getConfiguration().getString("rotations.path");
    }
  }

  public static class AutoRestart {

    public static boolean enabled() {
      return getConfiguration().getBoolean("restart.enabled", false);
    }

    public static Duration time() {
      return Duration.standardSeconds(getConfiguration().getInt("restart.time", 30)); // seconds
    }

    public static int matchLimit() {
      return getConfiguration().getInt("restart.match-limit", 30);
    }

    public static Duration taskInterval() {
      return Duration.standardSeconds(getConfiguration().getInt("restart.interval", 60)); // seconds
    }
  }

  public static class Join {
    public static boolean priorityKick() {
      return getConfiguration().getBoolean("join.priority-kick", true);
    }

    public static boolean midMatch() {
      return getConfiguration().getBoolean("join.mid-match", true);
    }

    public static boolean commitPlayers() {
      return getConfiguration().getBoolean("join.commit-players", false);
    }

    public static boolean capacity() {
      return getConfiguration().getBoolean("join.capacity.enabled", false);
    }

    public static boolean overfill() {
      return getConfiguration().getBoolean("join.capacity.overfill", false);
    }

    public static double overfillRatio() {
      return Math.max(1, getConfiguration().getDouble("join.capacity.overfill-ratio", 1.25));
    }
  }

  public static int minimumPlayers() {
    return getConfiguration().getInt("minimum-players", 1);
  }

  public static class Teams {
    public static int minimumPlayers() {
      return getConfiguration().getInt("teams.minimum-players", 0);
    }

    public static boolean requireEven() {
      return getConfiguration().getBoolean("teams.require-even", false);
    }

    public static boolean autoBalance() {
      return getConfiguration().getBoolean("teams.try-balance", true);
    }

    public static boolean allowChoose() {
      return getConfiguration().getBoolean("teams.allow-choose", true);
    }

    public static boolean allowSwitch() {
      // Team switching requires both team choosing and mid-match join to be enabled
      return Join.midMatch()
          && allowChoose()
          && getConfiguration().getBoolean("teams.allow-switch", true);
    }
  }

  public static class Broadcast {
    public static boolean enabled() {
      return getConfiguration().getBoolean("broadcast.enabled", true);
    }

    public static int /* seconds */ frequency() {
      int seconds = getConfiguration().getInt("broadcast.frequency", 600);
      if (seconds > 0) {
        return seconds;
      } else {
        return 600;
      }
    }
  }

  public static class ArrowRemoval {
    public static boolean enabled() {
      return getConfiguration().getBoolean("arrow-removal.enabled", true);
    }

    public static int /* seconds */ delay() {
      int seconds = getConfiguration().getInt("arrow-removal.delay", 10);
      if (seconds > 0) {
        return seconds;
      } else {
        return 10;
      }
    }
  }

  public static class Fishing {
    public static boolean disableTreasure() {
      return getConfiguration().getBoolean("fishing.disable-treasure", true);
    }
  }

  public static class Scoreboard {
    public static boolean showProximity() {
      return getConfiguration().getBoolean("scoreboard.show-proximity", false);
    }

    public static boolean preciseProgress() {
      return getConfiguration().getBoolean("scoreboard.precise-progress", false);
    }
  }

  public static class PlayerList implements Listener {
    private boolean enabled;
    private boolean playersSeeObservers;
    private @Nullable String datacenter;
    private @Nullable String server;
    private ImmutableList<Permission> permsByPriority;

    @EventHandler
    public void onConfigLoad(ConfigLoadEvent event) throws InvalidConfigurationException {
      this.load(event.getConfig());
    }

    public void load(Configuration config) throws InvalidConfigurationException {
      this.enabled = config.getBoolean("player-list.enabled", true);
      this.playersSeeObservers = config.getBoolean("player-list.players-see-observers", true);
      this.datacenter = config.getString("player-list.datacenter", null);
      this.server = config.getString("player-list.server", null);

      ImmutableList.Builder<Permission> perms = new ImmutableList.Builder<>();
      for (String permName : config.getStringList("player-list.priority")) {
        Permission permission = new Permission(permName);
        perms.add(permission);
      }
      this.permsByPriority = perms.build();
    }

    private static final PlayerList instance = new PlayerList();

    public static PlayerList get() {
      return instance;
    }

    public static boolean enabled() {
      return instance.enabled;
    }

    public static boolean playersSeeObservers() {
      return instance.playersSeeObservers;
    }

    public static ImmutableList<Permission> getPermsByPriority() {
      return instance.permsByPriority;
    }

    public static @Nullable String datacenter() {
      return instance.datacenter;
    }

    public static @Nullable String server() {
      return instance.server;
    }
  }

  public abstract static class Wool {
    public static boolean autoRefillWoolChests() {
      return getConfiguration().getBoolean("wool.auto-refill", true);
    }
  }

  public static class Motd {
    public static boolean enabled() {
      return getConfiguration().getBoolean("motd.enabled", true);
    }

    public static String format() {
      // Originally from PGMListener
      return ChatColor.translateAlternateColorCodes(
          '&',
          getConfiguration()
              .getString("motd.format", "{state.color}\u00BB &b{map.name} {state.color}\u00AB"));
    }

    public static class Colors {
      public static ChatColor idle() {
        return ChatColor.valueOf(
            getConfiguration()
                .getString("motd.colors.idle", "gray")
                .trim()
                .toUpperCase()
                .replace(' ', '_'));
      }

      public static ChatColor starting() {
        return ChatColor.valueOf(
            getConfiguration()
                .getString("motd.colors.starting", "yellow")
                .trim()
                .toUpperCase()
                .replace(' ', '_'));
      }

      public static ChatColor running() {
        return ChatColor.valueOf(
            getConfiguration()
                .getString("motd.colors.running", "green")
                .trim()
                .toUpperCase()
                .replace(' ', '_'));
      }

      public static ChatColor finished() {
        return ChatColor.valueOf(
            getConfiguration()
                .getString("motd.colors.finished", "red")
                .trim()
                .toUpperCase()
                .replace(' ', '_'));
      }
    }
  }
}
