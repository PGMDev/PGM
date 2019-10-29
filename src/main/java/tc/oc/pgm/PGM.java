package tc.oc.pgm;

import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.component.render.MatchNameRenderer;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.bossbar.BossBarModule;
import tc.oc.pgm.broadcast.BroadcastModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.commands.*;
import tc.oc.pgm.controlpoint.ControlPointModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.crafting.CraftingModule;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.damage.DamageModule;
import tc.oc.pgm.damage.DisableDamageModule;
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
import tc.oc.pgm.listeners.*;
import tc.oc.pgm.map.*;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchManager;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.WorldManager;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.modules.*;
import tc.oc.pgm.picker.PickerModule;
import tc.oc.pgm.portals.PortalModule;
import tc.oc.pgm.projectile.ProjectileModule;
import tc.oc.pgm.proximity.ProximityAlarmModule;
import tc.oc.pgm.rage.RageModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.renewable.RenewableModule;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.scoreboard.ScoreboardModule;
import tc.oc.pgm.scoreboard.SidebarModule;
import tc.oc.pgm.shield.ShieldMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.start.StartModule;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.timelimit.TimeLimitModule;
import tc.oc.pgm.tnt.TNTModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.RestartListener;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.pgm.worldborder.WorldBorderModule;
import tc.oc.server.Permissions;
import tc.oc.util.SemanticVersion;

public final class PGM extends JavaPlugin {
  private PGMUtil engine = new PGMUtil(this);

  private ModuleRegistry moduleRegistry;
  private MapLoader mapLoader;
  private MapLibrary mapLibrary;
  private Logger mapLogger;

  public MatchManager matchManager = null;
  public WorldManager worldManager = null;

  public PGM() {
    super();
  }

  public PGM(
      PluginLoader loader,
      Server server,
      PluginDescriptionFile description,
      File dataFolder,
      File file) {
    super(loader, server, description, dataFolder, file);
  }

  /** Map protocol this version of PGM supports. Follows the laws of semver.org. */
  public static final SemanticVersion MAP_PROTO_SUPPORTED = ProtoVersions.FILTER_FEATURES;

  private static PGM pgm;

  public static PGM get() {
    return pgm;
  }

  public static MatchManager getMatchManager() {
    return pgm == null ? null : pgm.matchManager;
  }

  public static MatchManager needMatchManager() {
    MatchManager mm = getMatchManager();
    if (mm == null) {
      throw new IllegalStateException("PGMMatchManager is not available");
    }
    return mm;
  }

  public static WorldManager getMapManager() {
    return pgm == null ? null : pgm.worldManager;
  }

  public Logger getMapLogger() {
    return mapLogger;
  }

  public MapLibrary getMapLibrary() {
    return mapLibrary;
  }

  private MatchTabManager matchTabManager;

  public Permission getParticipantPermissions() {
    return Permissions.PARTICIPANT;
  }

  private MapErrorTracker mapErrorTracker;

  public MapErrorTracker getMapErrorTracker() {
    return mapErrorTracker;
  }

  private void setupMapLogger() {
    mapLogger = Logger.getLogger(getLogger().getName() + ".maps");
    mapLogger.setUseParentHandlers(false);
    mapLogger.setParent(getLogger());

    this.mapErrorTracker = new MapErrorTracker();
    mapLogger.addHandler(this.mapErrorTracker);
    mapLogger.addHandler(new MapLogHandler());
  }

  @Override
  public void onEnable() {
    engine.onEnable();

    pgm = this;

    getLogger().setLevel(Level.INFO);

    getServer().getConsoleSender().addAttachment(this, Permissions.DEBUG, true);

    // Create objects that listen for config changes
    Config.PlayerList.register();

    // Copy the default configuration
    this.getConfig().options().copyDefaults(true);
    this.saveConfig();
    this.reloadConfig();
    this.setupMapLogger();

    try {
      this.moduleRegistry = this.createPGMModuleFactory();
    } catch (Throwable throwable) {
      // Is there something better than this we can throw?
      throw new RuntimeException(throwable);
    }

    // This permission doesn't exist yet, so we have to create it temporarily
    // while we load maps, so errors show up in the console.
    getServer().getPluginManager().addPermission(new Permission(Permissions.DEBUG));
    getServer().getPluginManager().removePermission(Permissions.DEBUG);

    this.worldManager = new WorldManager(this.getServer());
    registerEvents(this.worldManager);

    this.mapLoader = new MapLoader(this, this.getLogger(), this.moduleRegistry);
    this.mapLibrary = new MapLibrary(this.getLogger());

    try {
      this.matchManager = new MatchManager(this, mapLibrary, mapLoader, this.worldManager);
    } catch (MapNotFoundException e) {
      this.getLogger().log(Level.SEVERE, "PGM could not load any maps, server will shut down", e);
      this.getServer().shutdown();
      return;
    }

    MatchNameRenderer nameRenderer = new MatchNameRenderer(this);
    PGMUtil.get().setInnerNameRenderer(nameRenderer);
    registerEvents(nameRenderer);

    this.registerListeners();
    this.registerCommands();

    // cycle match in 0 ticks so it loads after other plugins are done
    this.getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            this,
            new Runnable() {
              @Override
              public void run() {
                if (PGM.this.matchManager.cycle(null, true, true) == null) {
                  getLogger().severe("Failed to load an initial match, shutting down");
                  getServer().shutdown();
                }
              }
            },
            0);

    if (Config.Broadcast.enabled()) {
      // periodically notify people of what map they're playing
      this.getServer()
          .getScheduler()
          .scheduleSyncRepeatingTask(
              this,
              new Runnable() {
                @Override
                public void run() {
                  Match match = PGM.this.matchManager.getCurrentMatch();
                  Bukkit.getConsoleSender()
                      .sendMessage(
                          ChatColor.DARK_PURPLE
                              + AllTranslations.get()
                                  .translate(
                                      "broadcast.currentlyPlaying",
                                      Bukkit.getConsoleSender(),
                                      match
                                              .getMap()
                                              .getInfo()
                                              .getShortDescription(Bukkit.getConsoleSender())
                                          + ChatColor.DARK_PURPLE));
                  for (MatchPlayer player : match.getPlayers()) {
                    player.sendMessage(
                        ChatColor.DARK_PURPLE
                            + AllTranslations.get()
                                .translate(
                                    "broadcast.currentlyPlaying",
                                    player.getBukkit(),
                                    match.getMap().getInfo().getShortDescription(player.getBukkit())
                                        + ChatColor.DARK_PURPLE));
                  }
                }
              },
              20,
              Config.Broadcast.frequency() * 20);
    }

    if (Config.PlayerList.enabled()) {
      this.matchTabManager = new MatchTabManager(this);
      this.matchTabManager.enable();
    }

    new RestartManager(this);
  }

  @Override
  public void onDisable() {
    if (this.matchTabManager != null) {
      this.matchTabManager.disable();
      this.matchTabManager = null;
    }

    if (this.matchManager != null) {
      this.matchManager.unloadAllMatches();
      this.matchManager = null;
    }

    engine.onDisable();
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    this.getServer().getPluginManager().callEvent(new ConfigLoadEvent(this.getConfig()));
  }

  public ModuleRegistry getModuleRegistry() {
    return moduleRegistry;
  }

  public ModuleRegistry createPGMModuleFactory() throws Throwable {
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
    new BlockTransformListener(this).registerEvents();

    this.registerEvents(new PGMListener(this, this.matchManager));
    this.registerEvents(new FormattingListener());
    this.registerEvents(new AntiGriefListener(this.matchManager));
    this.registerEvents(new ItemTransferListener());
    this.registerEvents(new KillStreakListener());
    this.registerEvents(new LongRangeTNTListener(this));
    this.registerEvents(new RestartListener(this));
    this.registerEvents(new WorldProblemListener(this));
    this.registerEvents(new MatchAnnouncer());
    this.registerEvents(new MotdListener());
  }

  public void registerCommands() {
    BasicBukkitCommandGraph graph = new BasicBukkitCommandGraph(new CommandModule(this));

    graph.getRootDispatcherNode().registerCommands(new ChatCommands());
    graph.getRootDispatcherNode().registerCommands(new MapCommands());
    graph.getRootDispatcherNode().registerCommands(new CycleCommands());
    graph.getRootDispatcherNode().registerCommands(new InventoryCommands());
    graph.getRootDispatcherNode().registerCommands(new GoalCommands());
    graph.getRootDispatcherNode().registerCommands(new JoinCommands());
    graph.getRootDispatcherNode().registerCommands(new StartCommands());
    graph.getRootDispatcherNode().registerCommands(new DestroyableCommands());
    graph.getRootDispatcherNode().registerNode("team").registerCommands(new TeamCommands());
    graph.getRootDispatcherNode().registerCommands(new AdminCommands());
    graph.getRootDispatcherNode().registerCommands(new ClassCommands());
    graph
        .getRootDispatcherNode()
        .registerNode("players", "ffa")
        .registerCommands(new FreeForAllCommands());
    graph.getRootDispatcherNode().registerCommands(new MapDevelopmentCommands());
    graph.getRootDispatcherNode().registerCommands(new MatchCommands());
    graph
        .getRootDispatcherNode()
        .registerNode("mode", "modes")
        .registerCommands(new ModeCommands());
    graph.getRootDispatcherNode().registerCommands(new TimeLimitCommands());

    new BukkitIntake(this, graph).register();
  }

  public void registerEvents(Listener listener) {
    this.getServer().getPluginManager().registerEvents(listener, this);
  }
}
