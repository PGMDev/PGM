package tc.oc.pgm;

import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.fluent.DispatcherNode;
import app.ashcon.intake.parametric.AbstractModule;
import app.ashcon.intake.parametric.provider.EnumProvider;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
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
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.bossbar.BossBarModule;
import tc.oc.pgm.broadcast.BroadcastModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.commands.AdminCommands;
import tc.oc.pgm.commands.ClassCommands;
import tc.oc.pgm.commands.CycleCommands;
import tc.oc.pgm.commands.DestroyableCommands;
import tc.oc.pgm.commands.FreeForAllCommands;
import tc.oc.pgm.commands.GoalCommands;
import tc.oc.pgm.commands.InventoryCommands;
import tc.oc.pgm.commands.JoinCommands;
import tc.oc.pgm.commands.MapCommands;
import tc.oc.pgm.commands.MapDevelopmentCommands;
import tc.oc.pgm.commands.MatchCommands;
import tc.oc.pgm.commands.ModeCommands;
import tc.oc.pgm.commands.RotationCommands;
import tc.oc.pgm.commands.SettingCommands;
import tc.oc.pgm.commands.StartCommands;
import tc.oc.pgm.commands.TeamCommands;
import tc.oc.pgm.commands.TimeLimitCommands;
import tc.oc.pgm.commands.provider.AudienceProvider;
import tc.oc.pgm.commands.provider.DurationProvider;
import tc.oc.pgm.commands.provider.MatchPlayerProvider;
import tc.oc.pgm.commands.provider.MatchProvider;
import tc.oc.pgm.commands.provider.PGMMapProvider;
import tc.oc.pgm.commands.provider.TeamMatchModuleProvider;
import tc.oc.pgm.commands.provider.VectorProvider;
import tc.oc.pgm.controlpoint.ControlPointModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.crafting.CraftingModule;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.damage.DamageModule;
import tc.oc.pgm.damage.DisableDamageModule;
import tc.oc.pgm.db.DatastoreCacheImpl;
import tc.oc.pgm.db.DatastoreImpl;
import tc.oc.pgm.death.DeathMessageMatchModule;
import tc.oc.pgm.destroyable.DestroyableModule;
import tc.oc.pgm.development.MapErrorTracker;
import tc.oc.pgm.doublejump.DoubleJumpModule;
import tc.oc.pgm.events.ConfigLoadEvent;
import tc.oc.pgm.fallingblocks.FallingBlocksModule;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.flag.FlagModule;
import tc.oc.pgm.gamerules.GameRulesModule;
import tc.oc.pgm.goals.GoalModule;
import tc.oc.pgm.hunger.HungerModule;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.killreward.KillRewardModule;
import tc.oc.pgm.kits.KitModule;
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
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.MapLoader;
import tc.oc.pgm.map.MapLogHandler;
import tc.oc.pgm.map.MapNotFoundException;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchManagerImpl;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.modules.ArrowRemovalModule;
import tc.oc.pgm.modules.DiscardPotionBottlesModule;
import tc.oc.pgm.modules.EventFilterMatchModule;
import tc.oc.pgm.modules.FriendlyFireRefundModule;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.modules.InternalModule;
import tc.oc.pgm.modules.ItemDestroyModule;
import tc.oc.pgm.modules.ItemKeepModule;
import tc.oc.pgm.modules.LaneModule;
import tc.oc.pgm.modules.MaxBuildHeightModule;
import tc.oc.pgm.modules.MobsModule;
import tc.oc.pgm.modules.ModifyBowProjectileModule;
import tc.oc.pgm.modules.MultiTradeMatchModule;
import tc.oc.pgm.modules.PlayableRegionModule;
import tc.oc.pgm.modules.TimeLockModule;
import tc.oc.pgm.modules.ToolRepairModule;
import tc.oc.pgm.picker.PickerModule;
import tc.oc.pgm.portals.PortalModule;
import tc.oc.pgm.prefix.PrefixRegistry;
import tc.oc.pgm.prefix.PrefixRegistryImpl;
import tc.oc.pgm.projectile.ProjectileModule;
import tc.oc.pgm.proximity.ProximityAlarmModule;
import tc.oc.pgm.rage.RageModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.renewable.RenewableModule;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.ShouldRestartTask;
import tc.oc.pgm.rotation.RandomPGMMapOrder;
import tc.oc.pgm.rotation.RotationManager;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.scoreboard.ScoreboardModule;
import tc.oc.pgm.scoreboard.SidebarModule;
import tc.oc.pgm.shield.ShieldMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.start.StartModule;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.timelimit.TimeLimitModule;
import tc.oc.pgm.tnt.TNTModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.pgm.worldborder.WorldBorderModule;
import tc.oc.util.SemanticVersion;

public final class PGMImpl extends JavaPlugin implements PGM {

  private MatchManager matchManager;
  private MatchTabManager matchTabManager;

  private Logger mapLogger;
  private MapErrorTracker mapErrorTracker;
  private MapLibrary mapLibrary;

  private IdentityProvider identityProvider;
  private NameRenderer nameRenderer;

  private PrefixRegistry prefixRegistry;
  private Datastore datastore;
  private Datastore datastoreCache;

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

  public IdentityProvider getIdentityProvider() {
    return identityProvider;
  }

  public NameRenderer getNameRenderer() {
    return nameRenderer;
  }

  @Override
  public Datastore getDatastore() {
    return datastore;
  }

  @Override
  public Datastore getDatastoreCache() {
    return datastoreCache;
  }

  public MatchManager getMatchManager() {
    return matchManager;
  }

  public MatchTabManager getMatchTabManager() {
    return matchTabManager;
  }

  public Logger getMapLogger() {
    return mapLogger;
  }

  public MapErrorTracker getMapErrorTracker() {
    return mapErrorTracker;
  }

  public MapLibrary getMapLibrary() {
    return mapLibrary;
  }

  @Override
  public PrefixRegistry getPrefixRegistry() {
    return prefixRegistry;
  }

  @Override
  public SemanticVersion getMapProtoSupported() {
    return ProtoVersions.FILTER_FEATURES;
  }

  @Override
  public void onEnable() {
    PGM.set(this);

    final Logger logger = getLogger();
    final Server server = getServer();

    logger.setLevel(Level.INFO);

    Permissions.register(server.getPluginManager());
    server.getConsoleSender().addAttachment(this, Permissions.ALL.getName(), true);

    registerEvents(Config.PlayerList.get());
    registerEvents(Config.Prefixes.get());
    registerEvents(Config.Experiments.get());
    getConfig().options().copyDefaults(true);
    saveConfig();
    // reloadConfig(); // getConfig() already reloads config

    mapLogger = Logger.getLogger(logger.getName() + ".maps");
    mapLogger.setUseParentHandlers(false);
    mapLogger.setParent(logger);

    mapErrorTracker = new MapErrorTracker();
    mapLogger.addHandler(mapErrorTracker);
    mapLogger.addHandler(new MapLogHandler());

    try {
      datastore = new DatastoreImpl(new File(getDataFolder(), "pgm.db"));
      datastoreCache = new DatastoreCacheImpl(datastore);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "PGM could not load SQL datastore", e);
      server.shutdown();
      return;
    }

    ModuleRegistry registry;
    try {
      registry = createPGMModuleFactory();
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "PGM could not load any modules, server will shut down", t);
      server.shutdown();
      return;
    }

    MapLoader mapLoader = new MapLoader(this, logger, registry);
    mapLibrary = new MapLibrary(logger);

    try {
      matchManager = new MatchManagerImpl(server, mapLibrary, mapLoader);

      if (Config.Rotations.areEnabled()) {
        matchManager.setMapOrder(
            new RotationManager(logger, new File(getDataFolder(), Config.Rotations.getPath())));
      } else {
        matchManager.setMapOrder(new RandomPGMMapOrder(matchManager));
      }

    } catch (MapNotFoundException e) {
      logger.log(Level.SEVERE, "PGM could not load any maps, server will shut down", e);
      server.shutdown();
      return;
    }

    identityProvider =
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

    MatchNameRenderer matchNameRenderer = new MatchNameRenderer(this);
    registerEvents(matchNameRenderer);
    nameRenderer = new CachingNameRenderer(matchNameRenderer);

    registerListeners();
    registerCommands();

    // Wait until the next tick so that all other plugins are finished.
    getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            this,
            () -> {
              if (!matchManager
                  .cycleMatch(null, matchManager.getMapOrder().popNextMap(), true)
                  .isPresent()) {
                logger.severe("PGM could not load an initial match, server will shut down");
                server.shutdown();
              }
            });

    if (Config.PlayerList.enabled()) {
      matchTabManager = new MatchTabManager(this);
      matchTabManager.enable();
    }

    prefixRegistry = new PrefixRegistryImpl();
    registerEvents(prefixRegistry);

    if (Config.AutoRestart.enabled()) {
      getServer().getScheduler().runTaskTimer(this, new ShouldRestartTask(this), 0, 20 * 60);
    }
  }

  @Override
  public void onDisable() {
    if (matchTabManager != null) {
      matchTabManager.disable();
      matchTabManager = null;
    }

    if (matchManager != null) {
      for (Match match : ImmutableSet.copyOf(matchManager.getMatches())) {
        matchManager.unloadMatch(match.getId());
      }
      matchManager = null;
    }
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    getServer().getPluginManager().callEvent(new ConfigLoadEvent(getConfig()));
  }

  private ModuleRegistry createPGMModuleFactory() throws Throwable {
    ModuleRegistry factory = new ModuleRegistry(this);

    factory.registerStatic(InfoModule.class);
    factory.registerFixtureMatchModule(EventFilterMatchModule.class);
    factory.registerStatic(TeamModule.class);
    factory.registerStatic(FreeForAllModule.class);
    factory.registerStatic(RegionModule.class);
    factory.registerStatic(FilterModule.class);
    factory.registerStatic(StartModule.class);
    factory.registerStatic(SpawnModule.class);
    factory.registerStatic(PlayableRegionModule.class);
    factory.registerStatic(CoreModule.class);
    factory.registerStatic(TimeLockModule.class);
    factory.registerStatic(WoolModule.class);
    factory.registerStatic(ScoreModule.class);
    factory.registerStatic(KitModule.class);
    factory.registerStatic(ItemDestroyModule.class);
    factory.registerStatic(ToolRepairModule.class);
    factory.registerStatic(TNTModule.class);
    factory.registerStatic(PortalModule.class);
    factory.registerStatic(MaxBuildHeightModule.class);
    factory.registerStatic(DestroyableModule.class);
    factory.registerStatic(ModifyBowProjectileModule.class);
    factory.registerStatic(MobsModule.class);
    factory.registerStatic(LaneModule.class);
    factory.registerStatic(TimeLimitModule.class);
    factory.registerStatic(HungerModule.class);
    factory.registerStatic(BlitzModule.class);
    factory.registerStatic(KillRewardModule.class);
    factory.registerFixtureMatchModule(MultiTradeMatchModule.class);
    factory.registerStatic(ClassModule.class);
    factory.registerStatic(DisableDamageModule.class);
    factory.registerStatic(RageModule.class);
    factory.registerStatic(FriendlyFireRefundModule.class);
    factory.registerStatic(ItemKeepModule.class);
    factory.registerStatic(BossBarModule.class);
    factory.registerStatic(BlockDropsModule.class);
    factory.registerStatic(RenewableModule.class);
    factory.registerStatic(InternalModule.class);
    factory.registerStatic(ProximityAlarmModule.class);
    factory.registerStatic(GameRulesModule.class);
    factory.registerStatic(ObjectiveModesModule.class);
    factory.registerStatic(ControlPointModule.class);
    factory.registerStatic(BroadcastModule.class);
    factory.registerStatic(FallingBlocksModule.class);
    factory.registerStatic(DoubleJumpModule.class);
    factory.registerStatic(FlagModule.class);
    factory.registerStatic(ArrowRemovalModule.class);
    factory.registerStatic(ProjectileModule.class);
    factory.registerStatic(DiscardPotionBottlesModule.class);
    factory.registerStatic(ScoreboardModule.class);
    factory.registerStatic(SidebarModule.class);
    factory.registerStatic(PickerModule.class);
    factory.registerStatic(GoalModule.class);
    factory.registerStatic(DamageModule.class);
    factory.registerStatic(WorldBorderModule.class);
    factory.register(CraftingModule.class, new CraftingModule.Factory());
    factory.register(ItemModifyModule.class, new ItemModifyModule.Factory());
    factory.registerFixtureMatchModule(SnapshotMatchModule.class);
    factory.register(DeathMessageMatchModule.class, new DeathMessageMatchModule.Factory());
    factory.registerFixtureMatchModule(TrackerMatchModule.class);
    factory.register(TerrainModule.class, new TerrainModule.Factory());
    factory.registerFixtureMatchModule(ShieldMatchModule.class);
    factory.register(ViewInventoryMatchModule.class, new ViewInventoryMatchModule.Factory());
    factory.register(JoinMatchModule.class, new JoinMatchModule.Factory());
    factory.register(CycleMatchModule.class, new CycleMatchModule.Factory());

    return factory;
  }

  private void registerListeners() {
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
    }

    private void configureProviders() {
      bind(Audience.class).toProvider(new AudienceProvider());
      bind(Match.class).toProvider(new MatchProvider(getMatchManager()));
      bind(MatchPlayer.class).toProvider(new MatchPlayerProvider(getMatchManager()));
      bind(PGMMap.class).toProvider(new PGMMapProvider(getMatchManager(), getMapLibrary()));
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
    node.registerCommands(new MapDevelopmentCommands());
    node.registerCommands(new MatchCommands());
    node.registerNode("mode", "modes").registerCommands(new ModeCommands());
    node.registerCommands(new TimeLimitCommands());
    node.registerCommands(new RotationCommands());
    node.registerCommands(new SettingCommands());

    new BukkitIntake(this, graph).register();
  }

  private void registerEvents(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }
}
