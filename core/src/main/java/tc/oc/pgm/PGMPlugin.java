package tc.oc.pgm;

import com.google.common.collect.Lists;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
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
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.module.Module;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.command.graph.CommandExecutor;
import tc.oc.pgm.command.graph.CommandGraph;
import tc.oc.pgm.community.command.CommunityCommandGraph;
import tc.oc.pgm.community.features.VanishManagerImpl;
import tc.oc.pgm.db.CacheDatastore;
import tc.oc.pgm.db.SQLDatastore;
import tc.oc.pgm.listeners.AntiGriefListener;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.listeners.FormattingListener;
import tc.oc.pgm.listeners.GeneralizingListener;
import tc.oc.pgm.listeners.ItemTransferListener;
import tc.oc.pgm.listeners.MatchAnnouncer;
import tc.oc.pgm.listeners.MotdListener;
import tc.oc.pgm.listeners.PGMListener;
import tc.oc.pgm.listeners.ServerPingDataListener;
import tc.oc.pgm.listeners.WorldProblemListener;
import tc.oc.pgm.map.MapLibraryImpl;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.match.NoopVanishManager;
import tc.oc.pgm.namedecorations.ConfigDecorationProvider;
import tc.oc.pgm.namedecorations.NameDecorationRegistry;
import tc.oc.pgm.namedecorations.NameDecorationRegistryImpl;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.RandomMapOrder;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.concurrent.BukkitExecutorService;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class PGMPlugin extends JavaPlugin implements PGM, Listener {

  private Config config;
  private Logger gameLogger;
  private Datastore datastore;
  private MapLibrary mapLibrary;
  private List<MapSourceFactory> mapSourceFactories;
  private MatchManager matchManager;
  private MatchTabManager matchTabManager;
  private MapOrder mapOrder;
  private NameDecorationRegistry nameDecorationRegistry;
  private ScheduledExecutorService executorService;
  private ScheduledExecutorService asyncExecutorService;
  private VanishManager vanishManager;

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
    try {
      PGM.set(this);
    } catch (IllegalArgumentException e) {
      return; // Indicates the plugin failed to load, so exit early
    }

    Modules.registerAll();
    Permissions.registerAll();

    final CommandSender console = getServer().getConsoleSender();
    console.addAttachment(this, Permissions.ALL.getName(), true);
    console.addAttachment(this, Permissions.DEBUG, false);
    console.recalculatePermissions();

    final Logger logger = getLogger();
    gameLogger = Logger.getLogger(logger.getName() + ".game");
    gameLogger.setUseParentHandlers(false);
    gameLogger.addHandler(new InGameHandler());
    gameLogger.setParent(logger);

    executorService = new BukkitExecutorService(this, false);
    asyncExecutorService = new BukkitExecutorService(this, true);

    mapSourceFactories = new ArrayList<>();
    mapLibrary = new MapLibraryImpl(gameLogger, mapSourceFactories);

    saveDefaultConfig(); // Writes a config file, if one does not exist.
    reloadConfig(); // Populates "this.config", if there is an error, will be null

    if (config == null) {
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    try {
      datastore = new SQLDatastore(config.getDatabaseUri(), config.getDatabaseMaxConnections());
    } catch (SQLException | TextException e) {
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    datastore = new CacheDatastore(datastore);

    try {
      mapLibrary.loadNewMaps(false).get(30, TimeUnit.SECONDS);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      e.printStackTrace();
    }

    if (!mapLibrary.getMaps().hasNext()) {
      PGMConfig.registerRemoteMapSource(mapSourceFactories, PGMConfig.DEFAULT_REMOTE_REPO);
      try {
        mapLibrary.loadNewMaps(false).get(30, TimeUnit.SECONDS);
      } catch (ExecutionException | InterruptedException | TimeoutException e) {
        e.printStackTrace();
      } finally {
        if (!mapLibrary.getMaps().hasNext()) {
          getServer().getPluginManager().disablePlugin(this);
          return;
        }
      }
    }

    if (config.getMapPoolFile() == null) {
      mapOrder = new RandomMapOrder(Lists.newArrayList(mapLibrary.getMaps()));
    } else {
      mapOrder = new MapPoolManager(logger, new File(config.getMapPoolFile()), datastore);
    }

    // FIXME: To avoid startup lag, we "prefetch" usernames after map pools are loaded.
    // Change MapPoolManager so it doesn't depend on all maps being loaded.
    for (MapInfo map : Lists.newArrayList(mapLibrary.getMaps())) {
      for (Contributor author : map.getAuthors()) {
        author.getName();
      }
      for (Contributor contributor : map.getContributors()) {
        contributor.getName();
      }
    }

    nameDecorationRegistry =
        new NameDecorationRegistryImpl(
            config.getGroups().isEmpty() ? null : new ConfigDecorationProvider());

    // Sometimes match folders need to be cleaned up if the server previously crashed
    final File[] worldDirs = getServer().getWorldContainer().listFiles();
    if (worldDirs != null) {
      for (File dir : worldDirs) {
        if (dir.isDirectory() && dir.getName().startsWith("match")) {
          FileUtils.delete(dir);
        }
      }
    }

    matchManager = new MatchManagerImpl(logger);

    vanishManager =
        config.isCommunityMode()
            ? new VanishManagerImpl(matchManager, executorService)
            : new NoopVanishManager();

    if (config.showTabList()) {
      matchTabManager = new MatchTabManager(this);
    }

    if (!config.getUptimeLimit().isNegative()) {
      asyncExecutorService.scheduleAtFixedRate(new ShouldRestartTask(), 0, 1, TimeUnit.MINUTES);
    }

    registerListeners();
    registerCommands();
  }

  @Override
  public void onDisable() {
    if (matchTabManager != null) matchTabManager.disable();
    if (matchManager != null) matchManager.getMatches().forEachRemaining(Match::unload);
    if (vanishManager != null) vanishManager.disable();
    if (executorService != null) executorService.shutdown();
    if (asyncExecutorService != null) asyncExecutorService.shutdown();
    if (datastore != null) datastore.close();
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    final boolean startup = config == null;
    try {
      config = new PGMConfig(getConfig(), getDataFolder());
    } catch (TextException e) {
      getGameLogger().log(Level.SEVERE, e.getLocalizedMessage(), e);
      return;
    }

    if (!startup) {
      getGameLogger()
          .log(
              Level.INFO, ChatColor.GREEN + TextTranslations.translate("admin.reloadConfig", null));
    }

    final Logger logger = getLogger();
    logger.setLevel(config.getLogLevel());

    mapSourceFactories.clear();
    mapSourceFactories.addAll(config.getMapSourceFactories());

    if (mapOrder != null) {
      mapOrder.reload();
    }
  }

  @Override
  public Config getConfiguration() {
    return config;
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
  public NameDecorationRegistry getNameDecorationRegistry() {
    return nameDecorationRegistry;
  }

  @Override
  public ScheduledExecutorService getExecutor() {
    return executorService;
  }

  @Override
  public ScheduledExecutorService getAsyncExecutor() {
    return asyncExecutorService;
  }

  @Override
  public VanishManager getVanishManager() {
    return vanishManager;
  }

  private void registerCommands() {
    final CommandGraph graph =
        config.isCommunityMode() ? new CommunityCommandGraph() : new CommandGraph();

    graph.register(vanishManager);
    graph.register(ChatDispatcher.get());

    new CommandExecutor(this, graph).register();
  }

  private void registerEvents(Object listener) {
    if (listener instanceof Listener) {
      getServer().getPluginManager().registerEvents((Listener) listener, this);
    }
  }

  private void registerListeners() {
    new BlockTransformListener(this).registerEvents();
    registerEvents(matchManager);
    if (matchTabManager != null) registerEvents(matchTabManager);
    registerEvents(vanishManager);
    registerEvents(nameDecorationRegistry);
    registerEvents(new GeneralizingListener(this));
    registerEvents(new PGMListener(this, matchManager, vanishManager));
    registerEvents(new FormattingListener());
    registerEvents(new AntiGriefListener(matchManager));
    registerEvents(new ItemTransferListener());
    registerEvents(new RestartListener(this, matchManager));
    registerEvents(new WorldProblemListener(this));
    registerEvents(new MatchAnnouncer());
    registerEvents(new MotdListener());
    registerEvents(new ServerPingDataListener(matchManager, mapOrder, getLogger(), vanishManager));
  }

  private class InGameHandler extends Handler {

    @Override
    public void publish(LogRecord record) {
      final String message = format(record);

      if (message != null) {
        getLogger().log(Level.INFO, ChatColor.stripColor(message));
        Bukkit.broadcast(message, Permissions.DEBUG);
      }

      if (message == null || message.contains("Unhandled")) {
        getLogger()
            .log(record.getLevel(), record.getThrown().getMessage(), record.getThrown().getCause());
      }
    }

    private String format(LogRecord record) {
      final Throwable thrown = record.getThrown();
      if (thrown == null) {
        return record.getMessage();
      }

      final TextException textErr = tryException(TextException.class, thrown);
      if (textErr != null) {
        return format(null, textErr.getLocalizedMessage(), textErr.getCause());
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
