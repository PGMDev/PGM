package tc.oc.pgm.platform.modern.modules;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import tc.oc.pgm.api.Modules;
import tc.oc.pgm.platform.modern.modules.kits.ModernKitMatchModule;
import tc.oc.pgm.platform.modern.modules.trim.TrimMatchModule;
import tc.oc.pgm.platform.modern.modules.trim.TrimModule;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointMatchModule;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernModuleRegistrar implements Modules.ModuleRegistrar {
  @Override
  public void registerModules(Modules modules) {
    modules.register(
        ModernEventFilterMatchModule.class, new ModernEventFilterMatchModule.Factory());
    modules.register(ModernKitMatchModule.class, new ModernKitMatchModule.Factory());
    modules.register(ModernMobsMatchModule.class, new ModernMobsMatchModule.Factory());
    modules.register(TrimModule.class, TrimMatchModule.class, new TrimModule.Factory());
    modules.register(WaypointMatchModule.class, WaypointMatchModule::new);
  }
}
