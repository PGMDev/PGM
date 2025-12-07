package tc.oc.pgm.platform.sportpaper.modules;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.platform.ModuleRegistrar;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpModuleRegistrar implements ModuleRegistrar {
  @Override
  public void registerModules(Modules modules) {}
}
