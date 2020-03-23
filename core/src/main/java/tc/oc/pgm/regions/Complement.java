package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;

public class Complement extends AbstractRegion {
  private final Region original;
  private final Region subtracted;

  public Complement(Region original, Region... subtracted) {
    this.original = original;
    this.subtracted = Union.of(subtracted);
  }

  @Override
  public boolean contains(Vector point) {
    return this.original.contains(point) && !this.subtracted.contains(point);
  }

  @Override
  public boolean isBlockBounded() {
    return this.original.isBlockBounded();
  }

  @Override
  public boolean isEmpty() {
    return this.original.isEmpty();
  }

  @Override
  public Bounds getBounds() {
    return Bounds.complement(this.original.getBounds(), this.subtracted.getBounds());
  }

  @Override
  public String toString() {
    return "Complement{original="
        + this.original.toString()
        + ",subtracted="
        + this.subtracted.toString()
        + "]}";
  }
}
