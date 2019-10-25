package tc.oc.pgm;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.joda.time.Duration;
import tc.oc.identity.Identity;
import tc.oc.identity.IdentityProvider;
import tc.oc.identity.RealIdentity;
import tc.oc.named.CachingNameRenderer;
import tc.oc.named.NameRenderer;
import tc.oc.named.NicknameRenderer;
import tc.oc.pgm.listeners.GeneralizingListener;
import tc.oc.pgm.listeners.InactivePlayerListener;
import tc.oc.server.ConfigUtils;
import tc.oc.server.Permissions;

public class PGMUtil implements Listener {

  private final JavaPlugin plugin;

  public PGMUtil(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  private static PGMUtil instance;

  public static PGMUtil get() {
    return instance;
  }

  public static JavaPlugin plugin() {
    return get().plugin;
  }

  private final IdentityProvider identityProvider =
      new IdentityProvider() {
        @Override
        public Identity getIdentity(Player player) {
          return new RealIdentity(player.getUniqueId(), player.getName());
        }

        @Override
        public Identity getIdentity(UUID playerId, String username, @Nullable String nickname) {
          return new RealIdentity(playerId, username);
        }
      };

  public IdentityProvider getIdentityProvider() {
    return identityProvider;
  }

  private NameRenderer nameRenderer;

  public NameRenderer getNameRenderer() {
    return nameRenderer;
  }

  public void setNameRenderer(NameRenderer nameRenderer) {
    this.nameRenderer = nameRenderer;
  }

  public void setInnerNameRenderer(NameRenderer nameRenderer) {
    setNameRenderer(new CachingNameRenderer(nameRenderer));
  }

  public Permission getObserverPermissions() {
    return Permissions.OBSERVER;
  }

  public void onEnable() {
    instance = this;

    this.setupPermissions();
    this.registerListeners();

    this.setInnerNameRenderer(new NicknameRenderer());
  }

  public void onDisable() {
    instance = null;
  }

  private void setupPermissions() {
    Permissions.register(plugin.getServer().getPluginManager());
    plugin.getServer().getConsoleSender().addAttachment(plugin, Permissions.ALL.getName(), true);
  }

  private void registerListeners() {
    PluginManager pluginManager = plugin.getServer().getPluginManager();

    pluginManager.registerEvents(new GeneralizingListener(plugin), plugin);

    this.registerInactivityListener();
  }

  private void registerInactivityListener() {
    Duration timeout = ConfigUtils.getDuration(plugin.getConfig(), "afk.timeout");
    Duration warning = ConfigUtils.getDuration(plugin.getConfig(), "afk.warning");
    Duration interval =
        ConfigUtils.getDuration(plugin.getConfig(), "afk.interval", Duration.standardSeconds(10));
    if (timeout != null) {
      plugin
          .getServer()
          .getPluginManager()
          .registerEvents(new InactivePlayerListener(plugin, timeout, warning, interval), plugin);
    }
  }
}
