package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;
import tc.oc.pgm.map.MapModuleContext;

public class StandardMapTag extends MapTag implements Predicate<MapModuleContext> {
  private final Predicate<MapModuleContext> ifApplicable;

  protected StandardMapTag(String name, Predicate<MapModuleContext> ifApplicable) {
    super(name);
    this.ifApplicable = checkNotNull(ifApplicable);
  }

  @Override
  public boolean test(MapModuleContext mapModuleContext) {
    return this.ifApplicable.test(mapModuleContext);
  }
}
