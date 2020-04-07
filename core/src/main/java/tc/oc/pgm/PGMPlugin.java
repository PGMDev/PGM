package tc.oc.pgm;

import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.fluent.DispatcherNode;
import app.ashcon.intake.parametric.AbstractModule;
import app.ashcon.intake.parametric.provider.EnumProvider;
import java.io.File;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.joda.time.Duration;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.module.Module;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.prefix.PrefixRegistry;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.commands.AdminCommands;
import tc.oc.pgm.commands.ClassCommands;
import tc.oc.pgm.commands.CycleCommands;
import tc.oc.pgm.commands.DestroyableCommands;
import tc.oc.pgm.commands.FreeForAllCommands;
import tc.oc.pgm.commands.GoalCommands;
import tc.oc.pgm.commands.InventoryCommands;
import tc.oc.pgm.commands.JoinCommands;
import tc.oc.pgm.commands.MapCommands;
import tc.oc.pgm.commands.MapPoolCommands;
import tc.oc.pgm.commands.MatchCommands;
import tc.oc.pgm.commands.ModeCommands;
import tc.oc.pgm.commands.ObserverCommands;
import tc.oc.pgm.commands.SettingCommands;
import tc.oc.pgm.commands.StartCommands;
import tc.oc.pgm.commands.StatsCommands;
import tc.oc.pgm.commands.TeamCommands;
import tc.oc.pgm.commands.TimeLimitCommands;
import tc.oc.pgm.commands.provider.AudienceProvider;
import tc.oc.pgm.commands.provider.DurationProvider;
import tc.oc.pgm.commands.provider.MapInfoProvider;
import tc.oc.pgm.commands.provider.MatchPlayerProvider;
import tc.oc.pgm.commands.provider.MatchProvider;
import tc.oc.pgm.commands.provider.SettingKeyProvider;
import tc.oc.pgm.commands.provider.TeamMatchModuleProvider;
import tc.oc.pgm.commands.provider.VectorProvider;
import tc.oc.pgm.community.commands.ModerationCommands;
import tc.oc.pgm.community.commands.ReportCommands;
import tc.oc.pgm.db.DatastoreCacheImpl;
import tc.oc.pgm.db.DatastoreImpl;
import tc.oc.pgm.events.ConfigLoadEvent;
import tc.oc.pgm.listeners.AntiGriefListener;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.listeners.FormattingListener;
import tc.oc.pgm.listeners.GeneralizingListener;
import tc.oc.pgm.listeners.ItemTransferListener;
import tc.oc.pgm.listeners.LongRangeTNTListener;
import tc.oc.pgm.listeners.MatchAnnouncer;
import tc.oc.pgm.listeners.MotdListener;
import tc.oc.pgm.listeners.PGMListener;
import tc.oc.pgm.listeners.ServerPingDataListener;
import tc.oc.pgm.listeners.WorldProblemListener;
import tc.oc.pgm.map.MapLibraryImpl;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.prefix.PrefixRegistryImpl;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.RandomMapOrder;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.util.FileUtils;
import tc.oc.util.bukkit.chat.Audience;
import tc.oc.util.xml.InvalidXMLException;

public class PGMPlugin extends JavaPlugin implements PGM, Listener {

  private Logger gameLogger;
  private Datastore datastore;
  private MapLibrary mapLibrary;
  private MatchManager matchManager;
  private MatchTabManager matchTabManager;
  private MapOrder mapOrder;
  private PrefixRegistry prefixRegistry;

  public PGMPlugin() {
    super();
  }

  public PGMPlugin(
      PluginLoader loader,
      Server server,
      PluginDescriptionFile description,
      File dataFolder,
      File file) {
    super(loader, server, description, dataFolder, file);
  }

  @Override
  public void onEnable() {
    PGM.set(this);
    Modules.registerAll();
    Permissions.registerAll();

    final Server server = getServer();
    server.getConsoleSender().addAttachment(this, Permissions.ALL.getName(), true);

    final Logger logger = getLogger();
    logger.setLevel(Config.Log.level());

    registerEvents(Config.Maps.get());
    registerEvents(Config.PlayerList.get());
    registerEvents(Config.Prefixes.get());
    registerEvents(Config.Experiments.get());

    try {
      getConfig().options().copyDefaults(true);
      saveConfig();
      reloadConfig();
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to create or save configuration", t);
    }

    try {
      datastore = new DatastoreImpl(new File(getDataFolder(), "pgm.db"));
      datastore = new DatastoreCacheImpl(datastore);
    } catch (SQLException e) {
      shutdown("Failed to initialize SQL database", e);
      return;
    }

    gameLogger = Logger.getLogger(logger.getName() + ".game");
    gameLogger.setUseParentHandlers(false);
    gameLogger.addHandler(new InGameHandler());
    gameLogger.setParent(logger);

    mapLibrary = new MapLibraryImpl(gameLogger, Config.Maps.get().getFactories());
    try {
      mapLibrary.loadNewMaps(false).get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException e) {
      // No-op
    } catch (ExecutionException e) {
      if (!mapLibrary.getMaps().hasNext()) {
        shutdown("Failed to load any maps", e.getCause());
        return;
      } else {
        logger.log(Level.WARNING, "Failed to load some maps", e.getCause());
      }
    }

    if (!mapLibrary.getMaps().hasNext()) {
      shutdown("Failed to load at least 1 map before timeout", null);
      return;
    }

    matchManager = new MatchManagerImpl(logger);

    if (Config.MapPools.areEnabled()) {
      mapOrder = new MapPoolManager(logger, new File(getDataFolder(), Config.MapPools.getPath()));
    } else {
      mapOrder = new RandomMapOrder();
    }

    prefixRegistry = new PrefixRegistryImpl();

    if (Config.PlayerList.enabled()) {
      matchTabManager = new MatchTabManager(this);
    }

    if (Config.AutoRestart.enabled()) {
      getServer().getScheduler().runTaskTimer(this, new ShouldRestartTask(this), 0, 20 * 60);
    }

    registerListeners();
    registerCommands();
  }

  @Override
  public void onDisable() {
    if (matchTabManager != null) matchTabManager.disable();
    if (matchManager != null) matchManager.getMatches().forEachRemaining(Match::unload);
    datastore = null;
    mapLibrary = null;
    matchManager = null;
    matchTabManager = null;
    prefixRegistry = null;

    // Sometimes match folders need to be cleaned up due to de-syncs
    for (File dir : getServer().getWorldContainer().listFiles()) {
      if (dir.isDirectory() && dir.getName().startsWith("match")) {
        FileUtils.delete(dir);
      }
    }
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    getServer().getPluginManager().callEvent(new ConfigLoadEvent(getConfig()));
  }

  private void shutdown(String message, @Nullable Throwable cause) {
    getLogger().log(Level.WARNING, message, cause);
    getServer().shutdown();
  }

  @Override
  public Logger getGameLogger() {
    return gameLogger;
  }

  @Override
  public Datastore getDatastore() {
    return datastore;
  }

  @Override
  public MapLibrary getMapLibrary() {
    return mapLibrary;
  }

  @Override
  public MapOrder getMapOrder() {
    return mapOrder;
  }

  @Override
  public MatchManager getMatchManager() {
    return matchManager;
  }

  @Override
  public MatchTabManager getMatchTabManager() {
    return matchTabManager;
  }

  @Override
  public PrefixRegistry getPrefixRegistry() {
    return prefixRegistry;
  }

  private class CommandModule extends AbstractModule {
    @Override
    protected void configure() {
      configureInstances();
      configureProviders();
    }

    private void configureInstances() {
      bind(PGM.class).toInstance(PGMPlugin.this);
      bind(MatchManager.class).toInstance(getMatchManager());
      bind(MapLibrary.class).toInstance(getMapLibrary());
      bind(MapOrder.class).toInstance(getMapOrder());
    }

    private void configureProviders() {
      final MatchPlayerProvider playerProvider = new MatchPlayerProvider(getMatchManager());
      bind(MatchPlayer.class).toProvider(playerProvider);
      bind(Audience.class).toProvider(new AudienceProvider(playerProvider));
      bind(Match.class).toProvider(new MatchProvider(getMatchManager()));
      bind(MapInfo.class)
          .toProvider(new MapInfoProvider(getMatchManager(), getMapLibrary(), getMapOrder()));
      bind(Duration.class).toProvider(new DurationProvider());
      bind(TeamMatchModule.class).toProvider(new TeamMatchModuleProvider(getMatchManager()));
      bind(Vector.class).toProvider(new VectorProvider());
      bind(SettingKey.class).toProvider(new SettingKeyProvider());
      bind(SettingValue.class).toProvider(new EnumProvider<>(SettingValue.class));
    }
  }

  private void registerCommands() {
    BasicBukkitCommandGraph graph = new BasicBukkitCommandGraph(new CommandModule());
    DispatcherNode node = graph.getRootDispatcherNode();

    final ChatDispatcher chat = new ChatDispatcher(getMatchManager());
    node.registerCommands(chat);
    registerEvents(chat);

    node.registerCommands(new MapCommands());
    node.registerCommands(new CycleCommands());
    node.registerCommands(new InventoryCommands());
    node.registerCommands(new GoalCommands());
    node.registerCommands(new JoinCommands());
    node.registerCommands(new StartCommands());
    node.registerCommands(new DestroyableCommands());
    node.registerNode("team").registerCommands(new TeamCommands());
    node.registerCommands(new AdminCommands());
    node.registerCommands(new ClassCommands());
    node.registerNode("players", "ffa").registerCommands(new FreeForAllCommands());
    node.registerCommands(new MatchCommands());
    node.registerNode("mode", "modes").registerCommands(new ModeCommands());
    node.registerCommands(new TimeLimitCommands());
    node.registerCommands(new SettingCommands());
    node.registerCommands(new ObserverCommands());
    node.registerCommands(new MapPoolCommands());
    node.registerCommands(new StatsCommands());

    // TODO: Community commands
    final ModerationCommands modCommands = new ModerationCommands(chat, getMatchManager());
    node.registerCommands(modCommands);
    registerEvents(modCommands);

    node.registerCommands(new ReportCommands());

    new BukkitIntake(this, graph).register();
  }

  private void registerEvents(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void registerListeners() {
    registerEvents((Listener) matchManager);
    if (matchTabManager != null) registerEvents(matchTabManager);
    registerEvents(prefixRegistry);
    registerEvents(new GeneralizingListener(this));
    new BlockTransformListener(this).registerEvents();
    registerEvents(new PGMListener(this, matchManager));
    registerEvents(new FormattingListener());
    registerEvents(new AntiGriefListener(matchManager));
    registerEvents(new ItemTransferListener());
    registerEvents(new LongRangeTNTListener(this));
    registerEvents(new RestartListener(this, matchManager));
    registerEvents(new WorldProblemListener(this));
    registerEvents(new MatchAnnouncer());
    registerEvents(new MotdListener());
    registerEvents(new ServerPingDataListener(matchManager, mapOrder, getLogger()));
  }

  private class InGameHandler extends Handler {

    @Override
    public void publish(LogRecord record) {
      final String message = format(record);
      if (message == null) { // Escalate to plugin logger when unable to format the error
        getLogger().log(record.getLevel(), record.getMessage(), record.getThrown());
      } else {
        Bukkit.broadcast(message, Permissions.DEBUG);
      }
    }

    private String format(LogRecord record) {
      final Throwable thrown = record.getThrown();
      if (thrown == null) {
        return record.getMessage();
      }

      final InvalidXMLException xmlErr = tryException(InvalidXMLException.class, thrown);
      if (xmlErr != null) {
        return format(xmlErr.getFullLocation(), xmlErr.getMessage(), xmlErr.getCause());
      }

      final MapException mapErr = tryException(MapException.class, thrown);
      if (mapErr != null) {
        return format(mapErr.getLocation(), mapErr.getMessage(), mapErr.getCause());
      }

      final ModuleLoadException moduleErr = tryException(ModuleLoadException.class, thrown);
      if (moduleErr != null) {
        final Class<? extends Module> module = moduleErr.getModule();
        return format(
            (module == null ? ModuleLoadException.class : module).getSimpleName(),
            moduleErr.getMessage(),
            moduleErr.getCause());
      }

      return null;
    }

    private String format(
        @Nullable String location, @Nullable String message, @Nullable Throwable cause) {
      if (cause != null && Objects.equals(message, cause.getMessage())) cause = null;
      if (message == null) message = "<unknown message>";
      if (location == null) {
        location = "<unknown location>";
      } else {
        location = location.replace(System.getProperty("user.dir"), "<home>");
      }

      return ChatColor.AQUA
          + location
          + ": "
          + ChatColor.RED
          + message
          + (cause == null ? "" : ", caused by: " + cause.getMessage());
    }

    private @Nullable <E> E tryException(Class<E> type, @Nullable Throwable thrown) {
      if (thrown == null) return null;
      return type.isInstance(thrown) ? (E) thrown : tryException(type, thrown.getCause());
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
  }
}
