package tc.oc.pgm.platform.modern.modules;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import tc.oc.pgm.api.Modules;
import tc.oc.pgm.platform.modern.modules.damage.ModernDamageMatchModule;
import tc.oc.pgm.platform.modern.modules.kits.ModernKitMatchModule;
import tc.oc.pgm.platform.modern.modules.tracker.ModernTrackerMatchModule;
import tc.oc.pgm.platform.modern.modules.trim.TrimMatchModule;
import tc.oc.pgm.platform.modern.modules.trim.TrimModule;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointMatchModule;
import tc.oc.pgm.platform.modern.particle.ParticleMatchModule;
import tc.oc.pgm.platform.modern.particle.ParticleModule;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernModuleRegistrar implements Modules.ModuleRegistrar {
  @Override
  public void registerModules(Modules modules) {
    modules.register(ModernDamageMatchModule.class, new ModernDamageMatchModule.Factory());
    modules.register(
        ModernEventFilterMatchModule.class, new ModernEventFilterMatchModule.Factory());
    modules.register(ModernKitMatchModule.class, new ModernKitMatchModule.Factory());
    modules.register(ModernMobsMatchModule.class, new ModernMobsMatchModule.Factory());
    modules.register(ModernTrackerMatchModule.class, new ModernTrackerMatchModule.Factory());
    modules.register(TrimModule.class, TrimMatchModule.class, new TrimModule.Factory());
    modules.register(WaypointMatchModule.class, WaypointMatchModule::new);
    modules.register(ParticleModule.class, ParticleMatchModule.class, new ParticleModule.Factory());
  }
}
