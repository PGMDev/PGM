package tc.oc.pgm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.bossbar.BossBarMatchModule;
import tc.oc.pgm.broadcast.BroadcastModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.controlpoint.ControlPointModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.crafting.CraftingModule;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.damage.DamageModule;
import tc.oc.pgm.damage.DisableDamageModule;
import tc.oc.pgm.death.DeathMessageMatchModule;
import tc.oc.pgm.destroyable.DestroyableModule;
import tc.oc.pgm.doublejump.DoubleJumpMatchModule;
import tc.oc.pgm.fallingblocks.FallingBlocksModule;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.flag.FlagModule;
import tc.oc.pgm.gamerules.GameRulesModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.hunger.HungerModule;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.killreward.KillRewardModule;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.modules.ArrowRemovalMatchModule;
import tc.oc.pgm.modules.DiscardPotionBottlesModule;
import tc.oc.pgm.modules.EventFilterMatchModule;
import tc.oc.pgm.modules.FriendlyFireRefundModule;
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
import tc.oc.pgm.picker.PickerMatchModule;
import tc.oc.pgm.portals.PortalModule;
import tc.oc.pgm.projectile.ProjectileModule;
import tc.oc.pgm.proximity.ProximityAlarmModule;
import tc.oc.pgm.rage.RageModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.renewable.RenewableModule;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.scoreboard.ScoreboardMatchModule;
import tc.oc.pgm.scoreboard.SidebarMatchModule;
import tc.oc.pgm.shield.ShieldMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.timelimit.TimeLimitModule;
import tc.oc.pgm.tnt.TNTModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.pgm.worldborder.WorldBorderModule;

public class ModulesImpl implements Modules {

  private static final Map<Class<? extends MapModule>, MapModuleFactory<? extends MapModule>>
      mapModuleFactories = new ConcurrentHashMap<>();
  private static final Map<Class<? extends MatchModule>, MatchModuleFactory<? extends MatchModule>>
      matchModuleFactories = Collections.synchronizedMap(new LinkedHashMap<>());

  static {
    registerMapModuleFactories();
    registerMatchModuleFactories();
  }

  @Override
  public Map<Class<? extends MapModule>, MapModuleFactory<? extends MapModule>>
      getMapModuleFactories() {
    return mapModuleFactories;
  }

  private static void registerMapModuleFactories() {
    mapModuleFactories.put(TeamModule.class, new TeamModule.Factory());
    mapModuleFactories.put(FreeForAllModule.class, new FreeForAllModule.Factory());
    mapModuleFactories.put(RegionModule.class, new RegionModule.Factory());
    mapModuleFactories.put(FilterModule.class, new FilterModule.Factory());
    mapModuleFactories.put(SpawnModule.class, new SpawnModule.Factory());
    mapModuleFactories.put(PlayableRegionModule.class, new PlayableRegionModule.Factory());
    mapModuleFactories.put(CoreModule.class, new CoreModule.Factory());
    mapModuleFactories.put(
        TimeLockModule.class, new TimeLockModule.Factory()); // Does not make MatchModules
    mapModuleFactories.put(WoolModule.class, new WoolModule.Factory());
    mapModuleFactories.put(ScoreModule.class, new ScoreModule.Factory());
    mapModuleFactories.put(KitModule.class, new KitModule.Factory());
    mapModuleFactories.put(ItemDestroyModule.class, new ItemDestroyModule.Factory());
    mapModuleFactories.put(ToolRepairModule.class, new ToolRepairModule.Factory());
    mapModuleFactories.put(TNTModule.class, new TNTModule.Factory());
    mapModuleFactories.put(PortalModule.class, new PortalModule.Factory());
    mapModuleFactories.put(MaxBuildHeightModule.class, new MaxBuildHeightModule.Factory());
    mapModuleFactories.put(CraftingModule.class, new CraftingModule.Factory());
    mapModuleFactories.put(ItemModifyModule.class, new ItemModifyModule.Factory());
    mapModuleFactories.put(
        TerrainModule.class, new TerrainModule.Factory()); // Does not make MatchModules
    mapModuleFactories.put(DestroyableModule.class, new DestroyableModule.Factory());
    mapModuleFactories.put(
        ModifyBowProjectileModule.class, new ModifyBowProjectileModule.Factory());
    mapModuleFactories.put(MobsModule.class, new MobsModule.Factory());
    mapModuleFactories.put(LaneModule.class, new LaneModule.Factory());
    mapModuleFactories.put(TimeLimitModule.class, new TimeLimitModule.Factory());
    mapModuleFactories.put(HungerModule.class, new HungerModule.Factory());
    mapModuleFactories.put(BlitzModule.class, new BlitzModule.Factory());
    mapModuleFactories.put(KillRewardModule.class, new KillRewardModule.Factory());
    mapModuleFactories.put(ClassModule.class, new ClassModule.Factory());
    mapModuleFactories.put(DisableDamageModule.class, new DisableDamageModule.Factory());
    mapModuleFactories.put(RageModule.class, new RageModule.Factory());
    mapModuleFactories.put(FriendlyFireRefundModule.class, new FriendlyFireRefundModule.Factory());
    mapModuleFactories.put(ItemKeepModule.class, new ItemKeepModule.Factory());
    mapModuleFactories.put(BlockDropsModule.class, new BlockDropsModule.Factory());
    mapModuleFactories.put(RenewableModule.class, new RenewableModule.Factory());
    mapModuleFactories.put(InternalModule.class, new InternalModule.Factory());
    mapModuleFactories.put(ProximityAlarmModule.class, new ProximityAlarmModule.Factory());
    mapModuleFactories.put(GameRulesModule.class, new GameRulesModule.Factory());
    mapModuleFactories.put(ObjectiveModesModule.class, new ObjectiveModesModule.Factory());
    mapModuleFactories.put(ControlPointModule.class, new ControlPointModule.Factory());
    mapModuleFactories.put(BroadcastModule.class, new BroadcastModule.Factory());
    mapModuleFactories.put(FallingBlocksModule.class, new FallingBlocksModule.Factory());
    mapModuleFactories.put(FlagModule.class, new FlagModule.Factory());
    mapModuleFactories.put(ProjectileModule.class, new ProjectileModule.Factory());
    mapModuleFactories.put(
        DiscardPotionBottlesModule.class, new DiscardPotionBottlesModule.Factory());
    mapModuleFactories.put(DamageModule.class, new DamageModule.Factory());
    mapModuleFactories.put(WorldBorderModule.class, new WorldBorderModule.Factory());
  }

  @Override
  public Map<Class<? extends MatchModule>, MatchModuleFactory<? extends MatchModule>>
      getModuleFactories() {
    return matchModuleFactories;
  }

  private static void registerMatchModuleFactories() {
    // Note: unlike map modules, these are loaded in the order defined below
    matchModuleFactories.put(BossBarMatchModule.class, BossBarMatchModule::new);
    matchModuleFactories.put(
        StartMatchModule.class, StartMatchModule::new); // must build after boss bars
    matchModuleFactories.put(EventFilterMatchModule.class, EventFilterMatchModule::new);
    matchModuleFactories.put(MultiTradeMatchModule.class, MultiTradeMatchModule::new);
    matchModuleFactories.put(SnapshotMatchModule.class, SnapshotMatchModule::new);
    matchModuleFactories.put(DeathMessageMatchModule.class, DeathMessageMatchModule::new);
    matchModuleFactories.put(TrackerMatchModule.class, TrackerMatchModule::new);
    matchModuleFactories.put(ShieldMatchModule.class, ShieldMatchModule::new);
    matchModuleFactories.put(ViewInventoryMatchModule.class, ViewInventoryMatchModule::new);
    matchModuleFactories.put(JoinMatchModule.class, JoinMatchModule::new);
    matchModuleFactories.put(CycleMatchModule.class, CycleMatchModule::new);
    matchModuleFactories.put(
        DoubleJumpMatchModule.class, DoubleJumpMatchModule::new); // Change to not be global
    matchModuleFactories.put(ArrowRemovalMatchModule.class, ArrowRemovalMatchModule::new);
    matchModuleFactories.put(ScoreboardMatchModule.class, ScoreboardMatchModule::new);
    matchModuleFactories.put(SidebarMatchModule.class, SidebarMatchModule::new);
    matchModuleFactories.put(PickerMatchModule.class, PickerMatchModule::new);
    matchModuleFactories.put(GoalMatchModule.class, GoalMatchModule::new);
  }
}
