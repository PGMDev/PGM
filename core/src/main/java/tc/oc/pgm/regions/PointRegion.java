package tc.oc.pgm.regions;

import java.util.Random;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class PointRegion
    implements RegionDefinition.HardStatic, RegionDefinition.MutableSource<PointRegion.Mutable> {

  protected final Vector position;

  public PointRegion(Vector position) {
    this.position = position;
  }

  public Vector getPosition() {
    return position;
  }

  @Override
  public boolean contains(Vector point) {
    return position.equals(point);
  }

  @Override
  public Bounds getBounds() {
    return new Bounds(position, position);
  }

  @Override
  public boolean isBlockBounded() {
    return true;
  }

  @Override
  public boolean canGetRandom() {
    return true;
  }

  public PointRegion.Mutable asMutableCopy() {
    return new Mutable(this.position.clone());
  }

  public static class Mutable extends PointRegion implements RegionDefinition.Mutable {
    public Mutable(Vector position) {
      super(position);
    }

    public Vector getMutablePosition() {
      return position;
    }
  }

  @Override
  public Vector getRandom(Random random) {
    return position.clone();
  }
}
