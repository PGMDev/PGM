package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

/** Wherever you go, here you are */
public class EverywhereRegion implements RegionDefinition.HardStatic {
  public static final EverywhereRegion INSTANCE = new EverywhereRegion();

  private EverywhereRegion() {}

  @Override
  public boolean contains(Vector point) {
    return true;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.unbounded();
  }
}
