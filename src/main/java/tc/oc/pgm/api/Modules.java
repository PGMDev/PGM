package tc.oc.pgm.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.ModuleFactory;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.blockdrops.BlockDropsMatchModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.bossbar.BossBarMatchModule;
import tc.oc.pgm.broadcast.BroadcastMatchModule;
import tc.oc.pgm.broadcast.BroadcastModule;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.controlpoint.ControlPointMatchModule;
import tc.oc.pgm.controlpoint.ControlPointModule;
import tc.oc.pgm.core.CoreMatchModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.crafting.CraftingMatchModule;
import tc.oc.pgm.crafting.CraftingModule;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.damage.DamageMatchModule;
import tc.oc.pgm.damage.DamageModule;
import tc.oc.pgm.damage.DisableDamageMatchModule;
import tc.oc.pgm.damage.DisableDamageModule;
import tc.oc.pgm.death.DeathMessageMatchModule;
import tc.oc.pgm.destroyable.DestroyableMatchModule;
import tc.oc.pgm.destroyable.DestroyableModule;
import tc.oc.pgm.doublejump.DoubleJumpMatchModule;
import tc.oc.pgm.fallingblocks.FallingBlocksMatchModule;
import tc.oc.pgm.fallingblocks.FallingBlocksModule;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.flag.FlagMatchModule;
import tc.oc.pgm.flag.FlagModule;
import tc.oc.pgm.gamerules.GameRulesMatchModule;
import tc.oc.pgm.gamerules.GameRulesModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.hunger.HungerMatchModule;
import tc.oc.pgm.hunger.HungerModule;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;
import tc.oc.pgm.itemmeta.ItemModifyMatchModule;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.killreward.KillRewardMatchModule;
import tc.oc.pgm.killreward.KillRewardModule;
import tc.oc.pgm.kits.KitMatchModule;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.modules.ArrowRemovalMatchModule;
import tc.oc.pgm.modules.DiscardPotionBottlesMatchModule;
import tc.oc.pgm.modules.DiscardPotionBottlesModule;
import tc.oc.pgm.modules.EventFilterMatchModule;
import tc.oc.pgm.modules.FriendlyFireRefundMatchModule;
import tc.oc.pgm.modules.FriendlyFireRefundModule;
import tc.oc.pgm.modules.InternalMatchModule;
import tc.oc.pgm.modules.InternalModule;
import tc.oc.pgm.modules.ItemDestroyMatchModule;
import tc.oc.pgm.modules.ItemDestroyModule;
import tc.oc.pgm.modules.ItemKeepMatchModule;
import tc.oc.pgm.modules.ItemKeepModule;
import tc.oc.pgm.modules.LaneMatchModule;
import tc.oc.pgm.modules.LaneModule;
import tc.oc.pgm.modules.MaxBuildHeightMatchModule;
import tc.oc.pgm.modules.MaxBuildHeightModule;
import tc.oc.pgm.modules.MobsMatchModule;
import tc.oc.pgm.modules.MobsModule;
import tc.oc.pgm.modules.ModifyBowProjectileMatchModule;
import tc.oc.pgm.modules.ModifyBowProjectileModule;
import tc.oc.pgm.modules.MultiTradeMatchModule;
import tc.oc.pgm.modules.PlayableRegionMatchModule;
import tc.oc.pgm.modules.PlayableRegionModule;
import tc.oc.pgm.modules.TimeLockModule;
import tc.oc.pgm.modules.ToolRepairMatchModule;
import tc.oc.pgm.modules.ToolRepairModule;
import tc.oc.pgm.picker.PickerMatchModule;
import tc.oc.pgm.portals.PortalMatchModule;
import tc.oc.pgm.portals.PortalModule;
import tc.oc.pgm.projectile.ProjectileMatchModule;
import tc.oc.pgm.projectile.ProjectileModule;
import tc.oc.pgm.proximity.ProximityAlarmMatchModule;
import tc.oc.pgm.proximity.ProximityAlarmModule;
import tc.oc.pgm.rage.RageMatchModule;
import tc.oc.pgm.rage.RageModule;
import tc.oc.pgm.regions.RegionMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.renewable.RenewableMatchModule;
import tc.oc.pgm.renewable.RenewableModule;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.scoreboard.ScoreboardMatchModule;
import tc.oc.pgm.scoreboard.SidebarMatchModule;
import tc.oc.pgm.shield.ShieldMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.timelimit.TimeLimitModule;
import tc.oc.pgm.tnt.TNTMatchModule;
import tc.oc.pgm.tnt.TNTModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.wool.WoolMatchModule;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.pgm.worldborder.WorldBorderMatchModule;
import tc.oc.pgm.worldborder.WorldBorderModule;

public interface Modules {

  Map<Class<? extends MapModule>, MapModuleFactory<? extends MapModule>> MAP =
      new ConcurrentHashMap<>();
  Map<Class<? extends MatchModule>, MatchModuleFactory<? extends MatchModule>> MATCH =
      new ConcurrentHashMap<>();
  Map<Class<? extends MapModule>, Class<? extends MatchModule>> MAP_TO_MATCH =
      new ConcurrentHashMap<>();

  static void register(Class<? extends MatchModule> match, MatchModuleFactory factory) {
    register(null, match, factory);
  }

  static void register(
      Class<? extends MapModule> map, Class<? extends MatchModule> match, ModuleFactory factory) {
    checkNotNull(factory);
    if (map == null) {
      if (!(factory instanceof MatchModuleFactory)) {
        throw new IllegalArgumentException(
            factory.getClass().getSimpleName()
                + " is not a "
                + MatchModuleFactory.class.getSimpleName());
      }
      MATCH.put(match, (MatchModuleFactory<? extends MatchModule>) factory);
    } else {
      if (!(factory instanceof MapModuleFactory)) {
        throw new IllegalArgumentException(
            factory.getClass().getSimpleName()
                + " is not a "
                + MapModuleFactory.class.getSimpleName());
      }
      MAP.put(map, (MapModuleFactory<? extends MapModule>) factory);
      if (match != null) {
        MAP_TO_MATCH.put(map, match);
      }
    }
  }

  static void registerAll() {
    // MatchModules that are always created
    register(BossBarMatchModule.class, BossBarMatchModule::new);
    register(EventFilterMatchModule.class, EventFilterMatchModule::new);
    register(MultiTradeMatchModule.class, MultiTradeMatchModule::new);
    register(DeathMessageMatchModule.class, DeathMessageMatchModule::new);
    register(TrackerMatchModule.class, TrackerMatchModule::new);
    register(ShieldMatchModule.class, ShieldMatchModule::new);
    register(ViewInventoryMatchModule.class, ViewInventoryMatchModule::new);
    register(CycleMatchModule.class, CycleMatchModule::new);
    register(DoubleJumpMatchModule.class, DoubleJumpMatchModule::new);
    register(ArrowRemovalMatchModule.class, ArrowRemovalMatchModule::new);

    // MatchModules that require other dependencies
    register(GoalMatchModule.class, new GoalMatchModule.Factory());
    register(ScoreboardMatchModule.class, new ScoreboardMatchModule.Factory());
    register(JoinMatchModule.class, new JoinMatchModule.Factory());
    register(StartMatchModule.class, new StartMatchModule.Factory());
    register(SnapshotMatchModule.class, new SnapshotMatchModule.Factory());
    register(SidebarMatchModule.class, new SidebarMatchModule.Factory());
    register(PickerMatchModule.class, new PickerMatchModule.Factory());

    // MapModules that create a MatchModule
    register(TeamModule.class, TeamMatchModule.class, new TeamModule.Factory());
    register(FreeForAllModule.class, FreeForAllMatchModule.class, new FreeForAllModule.Factory());
    register(RegionModule.class, RegionMatchModule.class, new RegionModule.Factory());
    register(FilterModule.class, FilterMatchModule.class, new FilterModule.Factory());
    register(SpawnModule.class, SpawnMatchModule.class, new SpawnModule.Factory());
    register(
        PlayableRegionModule.class,
        PlayableRegionMatchModule.class,
        new PlayableRegionModule.Factory());
    register(CoreModule.class, CoreMatchModule.class, new CoreModule.Factory());
    register(WoolModule.class, WoolMatchModule.class, new WoolModule.Factory());
    register(ScoreModule.class, ScoreMatchModule.class, new ScoreModule.Factory());
    register(KitModule.class, KitMatchModule.class, new KitModule.Factory());
    register(
        ItemDestroyModule.class, ItemDestroyMatchModule.class, new ItemDestroyModule.Factory());
    register(ToolRepairModule.class, ToolRepairMatchModule.class, new ToolRepairModule.Factory());
    register(TNTModule.class, TNTMatchModule.class, new TNTModule.Factory());
    register(PortalModule.class, PortalMatchModule.class, new PortalModule.Factory());
    register(
        MaxBuildHeightModule.class,
        MaxBuildHeightMatchModule.class,
        new MaxBuildHeightModule.Factory());
    register(CraftingModule.class, CraftingMatchModule.class, new CraftingModule.Factory());
    register(ItemModifyModule.class, ItemModifyMatchModule.class, new ItemModifyModule.Factory());
    register(
        DestroyableModule.class, DestroyableMatchModule.class, new DestroyableModule.Factory());
    register(
        ModifyBowProjectileModule.class,
        ModifyBowProjectileMatchModule.class,
        new ModifyBowProjectileModule.Factory());
    register(MobsModule.class, MobsMatchModule.class, new MobsModule.Factory());
    register(LaneModule.class, LaneMatchModule.class, new LaneModule.Factory());
    register(TimeLimitModule.class, TimeLimitMatchModule.class, new TimeLimitModule.Factory());
    register(HungerModule.class, HungerMatchModule.class, new HungerModule.Factory());
    register(BlitzModule.class, BlitzMatchModule.class, new BlitzModule.Factory());
    register(KillRewardModule.class, KillRewardMatchModule.class, new KillRewardModule.Factory());
    register(ClassModule.class, ClassMatchModule.class, new ClassModule.Factory());
    register(
        DisableDamageModule.class,
        DisableDamageMatchModule.class,
        new DisableDamageModule.Factory());
    register(RageModule.class, RageMatchModule.class, new RageModule.Factory());
    register(
        FriendlyFireRefundModule.class,
        FriendlyFireRefundMatchModule.class,
        new FriendlyFireRefundModule.Factory());
    register(ItemKeepModule.class, ItemKeepMatchModule.class, new ItemKeepModule.Factory());
    register(BlockDropsModule.class, BlockDropsMatchModule.class, new BlockDropsModule.Factory());
    register(RenewableModule.class, RenewableMatchModule.class, new RenewableModule.Factory());
    register(InternalModule.class, InternalMatchModule.class, new InternalModule.Factory());
    register(
        ProximityAlarmModule.class,
        ProximityAlarmMatchModule.class,
        new ProximityAlarmModule.Factory());
    register(GameRulesModule.class, GameRulesMatchModule.class, new GameRulesModule.Factory());
    register(
        ObjectiveModesModule.class,
        ObjectiveModesMatchModule.class,
        new ObjectiveModesModule.Factory());
    register(
        ControlPointModule.class, ControlPointMatchModule.class, new ControlPointModule.Factory());
    register(BroadcastModule.class, BroadcastMatchModule.class, new BroadcastModule.Factory());
    register(
        FallingBlocksModule.class,
        FallingBlocksMatchModule.class,
        new FallingBlocksModule.Factory());
    register(FlagModule.class, FlagMatchModule.class, new FlagModule.Factory());
    register(ProjectileModule.class, ProjectileMatchModule.class, new ProjectileModule.Factory());
    register(
        DiscardPotionBottlesModule.class,
        DiscardPotionBottlesMatchModule.class,
        new DiscardPotionBottlesModule.Factory());
    register(DamageModule.class, DamageMatchModule.class, new DamageModule.Factory());
    register(
        WorldBorderModule.class, WorldBorderMatchModule.class, new WorldBorderModule.Factory());

    // MapModules that do not create a MatchModule
    register(TerrainModule.class, null, new TerrainModule.Factory());

    // MapModules that are also MatchModules
    register(TimeLockModule.class, TimeLockModule.class, new TimeLockModule.Factory());
  }
}
