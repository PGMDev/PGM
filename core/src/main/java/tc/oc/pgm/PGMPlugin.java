package tc.oc.pgm;

import app.ashcon.intake.CommandException;
import app.ashcon.intake.InvalidUsageException;
import app.ashcon.intake.InvocationCommandException;
import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.fluent.CommandGraph;
import app.ashcon.intake.fluent.DispatcherNode;
import app.ashcon.intake.parametric.AbstractModule;
import app.ashcon.intake.parametric.provider.EnumProvider;
import app.ashcon.intake.util.auth.AuthorizationException;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
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
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
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
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
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
import tc.oc.pgm.commands.ListCommands;
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
import tc.oc.pgm.map.source.DefaultMapSourceFactory;
import tc.oc.pgm.map.source.SystemMapSourceFactory;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.match.NoopVanishManager;
import tc.oc.pgm.prefix.ConfigPrefixProvider;
import tc.oc.pgm.prefix.PrefixRegistryImpl;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.RandomMapOrder;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.chat.Audience;
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
  private PrefixRegistry prefixRegistry;
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
      getServer().getPluginManager().disablePlugin(this);
      return;
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

    prefixRegistry =
        new PrefixRegistryImpl(config.getGroups().isEmpty() ? null : new ConfigPrefixProvider());

    // Sometimes match folders need to be cleaned up if the server previously crashed
    for (File dir : getServer().getWorldContainer().listFiles()) {
      if (dir.isDirectory() && dir.getName().startsWith("match")) {
        FileUtils.delete(dir);
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

    try {
      config = new PGMConfig(getConfig(), getDataFolder());
    } catch (TextException e) {
      getGameLogger().log(Level.SEVERE, e.getLocalizedMessage(), e);
      return;
    }

    getGameLogger()
        .log(Level.INFO, ChatColor.GREEN + TextTranslations.translate("admin.reloadConfig", null));

    final Logger logger = getLogger();
    logger.setLevel(config.getLogLevel());

    for (String source : config.getMapSources()) {
      final MapSourceFactory factory;
      try {
        factory =
            source.equalsIgnoreCase("default")
                ? DefaultMapSourceFactory.INSTANCE
                : new SystemMapSourceFactory(source);
      } catch (Throwable t) {
        t.printStackTrace();
        continue;
      }

      mapSourceFactories.add(factory);
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
  public PrefixRegistry getPrefixRegistry() {
    return prefixRegistry;
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

  private class CommandRegistrar extends BukkitIntake {

    public CommandRegistrar(CommandGraph commandGraph) {
      super(PGMPlugin.this, commandGraph);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      final Audience audience = Audience.get(sender);

      try {
        return this.getCommandGraph()
            .getRootDispatcherNode()
            .getDispatcher()
            .call(this.getCommand(command, args), this.getNamespace(sender));
      } catch (AuthorizationException e) {
        audience.sendWarning(TranslatableComponent.of("misc.noPermission"));
      } catch (InvocationCommandException e) {
        if (e.getCause() instanceof TextException) {
          audience.sendWarning(((TextException) e.getCause()).getText());
        } else {
          audience.sendWarning(TextException.unknown(e).getText());
          e.printStackTrace();
        }
      } catch (InvalidUsageException e) {
        if (e.getMessage() != null) {
          audience.sendWarning(TextComponent.of(e.getMessage()));
        }

        if (e.isFullHelpSuggested()) {
          audience.sendMessage(
              TextComponent.of(
                  "/"
                      + Joiner.on(' ').join(e.getAliasStack())
                      + " "
                      + e.getCommand().getDescription().getUsage()));
        }
      } catch (CommandException e) {
        audience.sendMessage(TextComponent.of(e.getMessage()));
      }

      return false;
    }
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

    final ChatDispatcher chat = new ChatDispatcher(getMatchManager(), getVanishManager());
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

    if (config.isCommunityMode()) {
      final ModerationCommands modCommands =
          new ModerationCommands(chat, matchManager, vanishManager);
      node.registerCommands(modCommands);
      registerEvents(modCommands);

      node.registerCommands(vanishManager);
      registerEvents((Listener) vanishManager);

      node.registerCommands(new ReportCommands());
      node.registerCommands(new ListCommands(vanishManager));
    }

    new CommandRegistrar(graph).register();
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
    registerEvents(new PGMListener(this, matchManager, vanishManager));
    registerEvents(new FormattingListener());
    registerEvents(new AntiGriefListener(matchManager));
    registerEvents(new ItemTransferListener());
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
