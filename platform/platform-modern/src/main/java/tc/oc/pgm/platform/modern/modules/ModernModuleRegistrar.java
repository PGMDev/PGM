package tc.oc.pgm.platform.modern.modules;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import tc.oc.pgm.api.Modules;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernModuleRegistrar implements Modules.ModuleRegistrar {
  @Override
  public void registerModules(Modules modules) {
    modules.register(WaypointMatchModule.class, WaypointMatchModule::new);
  }
}
