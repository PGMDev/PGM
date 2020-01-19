package tc.oc.pgm;

import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.fluent.DispatcherNode;
import app.ashcon.intake.parametric.AbstractModule;
import app.ashcon.intake.parametric.provider.EnumProvider;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.joda.time.Duration;
import tc.oc.component.render.MatchNameRenderer;
import tc.oc.identity.Identity;
import tc.oc.identity.IdentityProvider;
import tc.oc.identity.RealIdentity;
import tc.oc.named.CachingNameRenderer;
import tc.oc.named.NameRenderer;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
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
import tc.oc.pgm.commands.MatchCommands;
import tc.oc.pgm.commands.ModeCommands;
import tc.oc.pgm.commands.ModerationCommands;
import tc.oc.pgm.commands.ObserverCommands;
import tc.oc.pgm.commands.SettingCommands;
import tc.oc.pgm.commands.StartCommands;
import tc.oc.pgm.commands.TeamCommands;
import tc.oc.pgm.commands.TimeLimitCommands;
import tc.oc.pgm.commands.provider.AudienceProvider;
import tc.oc.pgm.commands.provider.DurationProvider;
import tc.oc.pgm.commands.provider.MapInfoProvider;
import tc.oc.pgm.commands.provider.MatchPlayerProvider;
import tc.oc.pgm.commands.provider.MatchProvider;
import tc.oc.pgm.commands.provider.TeamMatchModuleProvider;
import tc.oc.pgm.commands.provider.VectorProvider;
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
import tc.oc.pgm.listeners.WorldProblemListener;
import tc.oc.pgm.map.MapLibraryImpl;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.prefix.PrefixRegistry;
import tc.oc.pgm.prefix.PrefixRegistryImpl;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.MapOrder;
import tc.oc.pgm.rotation.RandomMapOrder;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.teams.TeamMatchModule;

public final class PGMImpl extends JavaPlugin implements PGM, IdentityProvider, Listener {

  private Logger gameLogger;
  private Datastore datastore;
  private Datastore datastoreCache;
  private MapLibrary mapLibrary;
  private MatchFactory matchFactory;
  private MatchManager matchManager;
  private MatchTabManager matchTabManager;
  private MapOrder mapOrder;
  private MatchNameRenderer matchNameRenderer;
  private NameRenderer nameRenderer;
  private PrefixRegistry prefixRegistry;

  public PGMImpl() {
    super();
  }

  public PGMImpl(
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

    final Server server = getServer();
    Permissions.register(server.getPluginManager());
    server.getConsoleSender().addAttachment(this, Permissions.ALL.getName(), true);

    final Logger logger = getLogger();
    logger.setLevel(Config.Log.level());

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
      datastoreCache = new DatastoreCacheImpl(datastore);
    } catch (SQLException e) {
      shutdown("Failed to initialize SQL database", e);
      return;
    }

    gameLogger = Logger.getLogger(logger.getName() + ".game");
    gameLogger.setUseParentHandlers(false);
    gameLogger.setParent(logger);

    // FIXME: Use gameLogger
    mapLibrary = new MapLibraryImpl(logger, Config.Maps.sources());
    try {
      mapLibrary.loadNewMaps(false).get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException e) {
      if (!mapLibrary.getMaps().hasNext()) {
        shutdown("Failed to load at least 1 map before timeout", null);
        return;
      }
    } catch (ExecutionException e) {
      if (!mapLibrary.getMaps().hasNext()) {
        shutdown("Failed to load any maps", e.getCause());
        return;
      } else {
        logger.log(Level.WARNING, "Could not build some maps", e.getCause());
      }
    }

    matchManager = new MatchManagerImpl(logger, server);
    matchFactory = (MatchFactory) matchManager;
    mapOrder =
        new RandomMapOrder(matchManager); // TODO: RotationManager and use it to get first match

    MapContext map;
    for (int i = 0; ; i += 1) {
      final String id = mapOrder.popNextMap().getId();
      try {
        map = mapLibrary.loadExistingMap(id).get(5, TimeUnit.SECONDS);
        break;
      } catch (Throwable t) {
        getLogger().log(Level.WARNING, "Failed to load map: " + id, t);
        if (i < 5) continue;
        shutdown("Failed to load any maps", t);
        return;
      }
    }

    prefixRegistry = new PrefixRegistryImpl();
    matchNameRenderer = new MatchNameRenderer(matchManager);
    nameRenderer = new CachingNameRenderer(matchNameRenderer);

    if (Config.PlayerList.enabled()) {
      matchTabManager = new MatchTabManager(this);
    }

    if (Config.AutoRestart.enabled()) {
      getServer().getScheduler().runTaskTimer(this, new ShouldRestartTask(this), 0, 20 * 60);
    }

    registerListeners();
    registerCommands();

    matchFactory
        .initMatch(map)
        .whenComplete(
            (Match match, Throwable err) -> {
              try {
                if (err != null) throw err;
                matchFactory.moveMatch(null, match);
              } catch (Throwable t) {
                shutdown("Unable to load match", t);
              }
            });
  }

  @Override
  public void onDisable() {
    if (matchTabManager != null) matchTabManager.disable();
    if (matchManager != null) matchManager.getMatches().iterator().forEachRemaining(Match::unload);
    if (datastore != null) datastore.shutdown();
    if (datastoreCache != null) datastoreCache.shutdown();
    datastore = null;
    datastoreCache = null;
    mapLibrary = null;
    matchManager = null;
    matchTabManager = null;
    nameRenderer = null;
    prefixRegistry = null;
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    getServer().getPluginManager().callEvent(new ConfigLoadEvent(getConfig()));
  }

  private void shutdown(String message, @Nullable Throwable cause) {
    getLogger().log(Level.SEVERE, message, cause);
    getServer().shutdown();
  }

  @Override
  public Identity getIdentity(Player player) {
    return new RealIdentity(player.getUniqueId(), player.getName());
  }

  @Override
  public Identity getIdentity(UUID playerId, String username, @Nullable String nickname) {
    return new RealIdentity(playerId, username);
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
  public Datastore getDatastoreCache() {
    return datastoreCache;
  }

  @Override
  public MatchFactory getMatchFactory() {
    return matchFactory;
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
  public NameRenderer getNameRenderer() {
    return nameRenderer;
  }

  @Override
  public PrefixRegistry getPrefixRegistry() {
    return prefixRegistry;
  }

  @Override
  public IdentityProvider getIdentityProvider() {
    return this;
  }

  private class CommandModule extends AbstractModule {
    @Override
    protected void configure() {
      configureInstances();
      configureProviders();
    }

    private void configureInstances() {
      bind(PGM.class).toInstance(PGMImpl.this);
      bind(MatchManager.class).toInstance(getMatchManager());
      bind(MapLibrary.class).toInstance(getMapLibrary());
      bind(MapOrder.class).toInstance(getMapOrder());
    }

    private void configureProviders() {
      bind(Audience.class).toProvider(new AudienceProvider());
      bind(Match.class).toProvider(new MatchProvider(getMatchManager()));
      bind(MatchPlayer.class).toProvider(new MatchPlayerProvider(getMatchManager()));
      bind(MapInfo.class)
          .toProvider(new MapInfoProvider(getMatchManager(), getMapLibrary(), getMapOrder()));
      bind(Duration.class).toProvider(new DurationProvider());
      bind(TeamMatchModule.class).toProvider(new TeamMatchModuleProvider(getMatchManager()));
      bind(Vector.class).toProvider(new VectorProvider());
      bind(SettingKey.class).toProvider(new EnumProvider<>(SettingKey.class));
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
    node.registerCommands(new ModerationCommands());
    node.registerCommands(new ObserverCommands());

    new BukkitIntake(this, graph).register();
  }

  private void registerEvents(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void registerListeners() {
    if (matchTabManager != null) registerEvents(matchTabManager);
    registerEvents(prefixRegistry);
    registerEvents(matchNameRenderer);
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
  }
}
