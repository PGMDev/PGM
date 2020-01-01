package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;
import tc.oc.pgm.map.PGMMap;

public class StandardMapTag extends MapTag implements Predicate<PGMMap> {
  private final Predicate<PGMMap> ifApplicable;

  protected StandardMapTag(String name, Predicate<PGMMap> ifApplicable) {
    super(name);
    this.ifApplicable = checkNotNull(ifApplicable);
  }

  @Override
  public boolean test(PGMMap pgmMap) {
    return this.ifApplicable.test(pgmMap);
  }
}
