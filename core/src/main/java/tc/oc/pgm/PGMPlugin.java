package tc.oc.pgm;

import com.google.common.collect.Lists;
import fr.minuskube.inv.InventoryManager;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.module.Module;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.command.util.PGMCommandGraph;
import tc.oc.pgm.db.CacheDatastore;
import tc.oc.pgm.db.SQLDatastore;
import tc.oc.pgm.db.SqlUsernameResolver;
import tc.oc.pgm.integrations.SimpleVanishIntegration;
import tc.oc.pgm.listeners.AntiGriefListener;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.listeners.FormattingListener;
import tc.oc.pgm.listeners.JoinLeaveAnnouncer;
import tc.oc.pgm.listeners.MatchAnnouncer;
import tc.oc.pgm.listeners.MotdListener;
import tc.oc.pgm.listeners.PGMListener;
import tc.oc.pgm.listeners.ServerPingDataListener;
import tc.oc.pgm.listeners.WorldProblemListener;
import tc.oc.pgm.map.MapLibraryImpl;
import tc.oc.pgm.map.includes.MapIncludeProcessorImpl;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.namedecorations.ConfigDecorationProvider;
import tc.oc.pgm.namedecorations.NameDecorationRegistry;
import tc.oc.pgm.namedecorations.NameDecorationRegistryImpl;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.RandomMapOrder;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.concurrent.BukkitExecutorService;
import tc.oc.pgm.util.listener.AfkTracker;
import tc.oc.pgm.util.listener.ItemTransferListener;
import tc.oc.pgm.util.listener.PlayerBlockListener;
import tc.oc.pgm.util.listener.PlayerMoveListener;
import tc.oc.pgm.util.listener.TNTMinecartPlacementListener;
import tc.oc.pgm.util.parser.SyntaxException;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.tablist.TablistResizer;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.util.usernames.ApiUsernameResolver;
import tc.oc.pgm.util.usernames.BukkitUsernameResolver;
import tc.oc.pgm.util.usernames.UsernameResolvers;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class PGMPlugin extends JavaPlugin implements PGM, Listener {

  private Config config;
  private Logger gameLogger;
  private Datastore datastore;
  private MapLibrary mapLibrary;
  private MapIncludeProcessor mapIncludeProcessor;
  private List<MapSourceFactory> mapSourceFactories;
  private MatchManager matchManager;
  private MatchTabManager matchTabManager;
  private MapOrder mapOrder;
  private NameDecorationRegistry nameDecorationRegistry;
  private ScheduledExecutorService executorService;
  private ScheduledExecutorService asyncExecutorService;
  private ChatManager chatManager;
  private InventoryManager inventoryManager;
  private AfkTracker afkTracker;

  public PGMPlugin() {
    super();
  }

  // Used by RuntimePluginLoader in PGMServer
  public PGMPlugin(
      JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
    super(loader, description, dataFolder, file);
  }

  @Override
  public void onEnable() {
    try {
      PGM.set(this);
    } catch (IllegalArgumentException e) {
      return; // Indicates the plugin failed to load, so exit early
    }

    // Sanity test PGM is running on a supported version before doing any work
    try {
      Platform.init();
    } catch (Throwable t) {
      getLogger().log(Level.SEVERE, "Failed to initialize PGM platform", t);
      getServer().getPluginManager().disablePlugin(this);
    }
    // Fix before any audiences have the chance of creating
    ViaUtils.removeViaChatFacet();

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
    mapIncludeProcessor = new MapIncludeProcessorImpl(gameLogger);
    mapLibrary = new MapLibraryImpl(gameLogger, mapSourceFactories, mapIncludeProcessor);

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

    UsernameResolvers.setResolvers(
        new BukkitUsernameResolver(),
        new SqlUsernameResolver((SQLDatastore) datastore),
        new ApiUsernameResolver());

    datastore = new CacheDatastore(datastore);

    if (!loadInitialMaps()) {
      logger.warning("No maps found, adding default repository as a fallback.");
      PGMConfig.registerRemoteMapSource(mapSourceFactories, PGMConfig.DEFAULT_REMOTE_REPO);
      if (!loadInitialMaps()) {
        logger.severe("No maps were loaded in time, PGM will be disabled");
        getServer().getPluginManager().disablePlugin(this);
        return;
      }
    }

    if (config.getMapPoolFile() != null) {
      MapPoolManager manager =
          new MapPoolManager(logger, config.getMapPoolFile().toFile(), datastore);
      var pool = manager.getActiveMapPool();
      if (pool != null) {
        if (!pool.getMaps().isEmpty()) mapOrder = manager;
        else logger.severe("Active pool has no maps. Falling back to a random pool.");
      }
    }
    if (mapOrder == null) mapOrder = new RandomMapOrder(Lists.newArrayList(mapLibrary.getMaps()));

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

    nameDecorationRegistry = new NameDecorationRegistryImpl(
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

    if (config.isVanishEnabled())
      Integration.setVanishIntegration(new SimpleVanishIntegration(matchManager, executorService));

    inventoryManager = new InventoryManager(this);
    afkTracker = new AfkTracker(this);

    if (config.showTabList()) {
      matchTabManager = new MatchTabManager(this);
    }

    if (config.resizeTabList()) {
      if (this.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
        TablistResizer.registerAdapter(this);
      } else {
        logger.warning("ProtocolLib is required when 'ui.resize' is enabled");
      }
    }

    if (!config.getUptimeLimit().isNegative()) {
      asyncExecutorService.scheduleAtFixedRate(new ShouldRestartTask(), 0, 1, TimeUnit.MINUTES);
    }

    chatManager = new ChatManager();

    registerListeners();
    registerCommands();
  }

  @Override
  public void onDisable() {
    if (matchTabManager != null) matchTabManager.disable();
    if (matchManager != null) matchManager.getMatches().forEachRemaining(Match::unload);
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
          .log(Level.INFO, ChatColor.GREEN + TextTranslations.translate("admin.reloadConfig"));
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
  public ChunkGenerator getDefaultWorldGenerator(final String worldName, final String id) {
    return NullChunkGenerator.INSTANCE;
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
  public InventoryManager getInventoryManager() {
    return inventoryManager;
  }

  @Override
  public AfkTracker getAfkTracker() {
    return afkTracker;
  }

  @Override
  public ChatManager getChatManager() {
    return chatManager;
  }

  private void registerCommands() {
    try {
      new PGMCommandGraph(this);
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Exception registering commands", e);
    }
  }

  private void registerEvents(Object listener) {
    if (listener instanceof Listener) {
      getServer().getPluginManager().registerEvents((Listener) listener, this);
    }
  }

  private void registerListeners() {
    Platform.MANIFEST.onEnable(this);

    registerEvents(new PlayerBlockListener());
    registerEvents(new PlayerMoveListener());
    registerEvents(new ItemTransferListener());
    registerEvents(new TNTMinecartPlacementListener());
    new BlockTransformListener(this).registerEvents();
    registerEvents(matchManager);
    inventoryManager.init();
    registerEvents(afkTracker);
    if (matchTabManager != null) registerEvents(matchTabManager);
    registerEvents(nameDecorationRegistry);
    registerEvents(new PGMListener(this, matchManager));
    registerEvents(new FormattingListener());
    registerEvents(new AntiGriefListener(matchManager));
    registerEvents(new RestartListener(this, matchManager));
    registerEvents(new WorldProblemListener(this));
    registerEvents(new MatchAnnouncer());
    registerEvents(new MotdListener());
    registerEvents(new ServerPingDataListener(matchManager, mapOrder, getLogger()));
    registerEvents(new JoinLeaveAnnouncer(matchManager));
    registerEvents(chatManager);
  }

  private boolean loadInitialMaps() {
    try {
      mapLibrary.loadNewMaps(false).get(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      getLogger().warning("Loading all maps took >30s, other maps will keep loading async.");
    } catch (ExecutionException | InterruptedException e) {
      getLogger().log(Level.WARNING, "Exception loading maps", e);
    }
    return mapLibrary.getMaps().hasNext();
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
            .log(
                record.getLevel(),
                record.getThrown().getMessage(),
                record.getThrown().getCause());
      }
    }

    private String format(LogRecord record) {
      final Throwable thrown = record.getThrown();
      if (thrown == null) {
        return record.getMessage();
      }

      final InvalidXMLException xmlErr = tryException(InvalidXMLException.class, thrown);
      final MapException mapErr = tryException(MapException.class, thrown);
      final ModuleLoadException moduleErr = tryException(ModuleLoadException.class, thrown);

      String location = null;
      if (xmlErr != null) {
        location = xmlErr.getFullLocation();
      } else if (mapErr != null) {
        location = mapErr.getLocation();
      } else if (moduleErr != null) {
        final Class<? extends Module> module = moduleErr.getModule();
        location = (module == null ? ModuleLoadException.class : module).getSimpleName();
      }

      final TextException textErr = tryException(TextException.class, thrown);

      Throwable cause = thrown.getCause();
      String message = thrown.getMessage();
      String detail = null;
      if (textErr != null) {
        cause = null;
        message = textErr.getLocalizedMessage();
      } else if (xmlErr != null) {
        cause = xmlErr.getCause();
        message = xmlErr.getMessage();

        if (cause instanceof SyntaxException se && xmlErr.getNode() != null) {
          String value = xmlErr.getNode().getValue();

          int start = se.getStartIdx();
          if (start == -1 || start > value.length()) start = value.length();
          int end = se.getEndIdx();
          if (end == -1 || end < start) end = value.length();

          detail = value.substring(0, start)
              + ChatColor.RED
              + ChatColor.UNDERLINE
              + value.substring(start, end)
              + ChatColor.RESET
              + ChatColor.RED
              + value.substring(end);
        }
      } else if (mapErr != null) {
        cause = mapErr.getCause();
        message = mapErr.getMessage();
      } else if (moduleErr != null) {
        cause = moduleErr.getCause();
        message = moduleErr.getMessage();
      }

      return format(location, message, detail, cause);
    }

    private String format(
        @Nullable String location,
        @Nullable String message,
        @Nullable String detail,
        @Nullable Throwable cause) {
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
          + (cause == null ? "" : ", caused by: " + cause.getMessage())
          + (detail == null ? "" : ", detail: " + detail);
    }

    private @Nullable <E> E tryException(Class<E> type, @Nullable Throwable thrown) {
      if (thrown == null) return null;
      return type.isInstance(thrown) ? type.cast(thrown) : tryException(type, thrown.getCause());
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
  }
}
