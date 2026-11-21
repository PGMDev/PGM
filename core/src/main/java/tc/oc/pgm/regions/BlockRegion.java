package tc.oc.pgm.regions;

import java.util.Random;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class BlockRegion
    implements RegionDefinition.HardStatic, RegionDefinition.MutableSource<BlockRegion.Mutable> {
  protected final Vector location;

  public BlockRegion(Vector block) {
    this.location = new Vector(block.getBlockX(), block.getBlockY(), block.getBlockZ());
  }

  @Override
  public boolean contains(Vector point) {
    return this.location.getBlockX() == point.getBlockX()
        && this.location.getBlockY() == point.getBlockY()
        && this.location.getBlockZ() == point.getBlockZ();
  }

  @Override
  public boolean canGetRandom() {
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    return true;
  }

  @Override
  public Bounds getBounds() {
    return new Bounds(this.location, this.location.clone().add(new Vector(1, 1, 1)));
  }

  @Override
  public Vector getRandom(Random random) {
    double dx = random.nextDouble();
    double dy = random.nextDouble();
    double dz = random.nextDouble();
    return this.location.clone().add(new Vector(dx, dy, dz));
  }

  public BlockRegion.Mutable asMutableCopy() {
    return new Mutable(this.location.clone());
  }

  public static class Mutable extends BlockRegion implements RegionDefinition.Mutable {
    public Mutable(Vector location) {
      super(location);
    }

    public Vector getMutableLocation() {
      return location;
    }
  }

  @Override
  public String toString() {
    return "BlockRegion{location=[" + this.location + "]}";
  }
}
