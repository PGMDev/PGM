package tc.oc.pgm.regions;

import java.util.Random;
import lombok.Getter;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

@Getter
public class PointRegion implements RegionDefinition.HardStatic {

  private final Vector position;

  public PointRegion(Vector position) {
    this.position = position;
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

  @Override
  public Vector getRandom(Random random) {
    return position.clone();
  }
}
