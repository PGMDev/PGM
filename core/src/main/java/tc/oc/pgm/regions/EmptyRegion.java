package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class EmptyRegion implements RegionDefinition.HardStatic {
  public static final EmptyRegion INSTANCE = new EmptyRegion();

  private EmptyRegion() {}

  @Override
  public boolean contains(Vector point) {
    return false;
  }

  @Override
  public boolean isBlockBounded() {
    return true;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.empty();
  }
}
