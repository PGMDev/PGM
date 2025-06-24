package tc.oc.pgm.regions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

public class Complement implements RegionDefinition.Static {
  private final Region original;
  private final Region subtracted;

  public Complement(Region original, Region... subtracted) {
    this.original = original;
    this.subtracted = Union.of(subtracted);
  }

  @Override
  public boolean contains(Vector point) {
    return this.original.getStatic().contains(point)
        && !this.subtracted.getStatic().contains(point);
  }

  @Override
  public boolean contains(Location point) {
    return this.original.contains(point) && !this.subtracted.contains(point);
  }

  @Override
  public boolean isBlockBounded() {
    return this.original.isBlockBounded();
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return new Complement(original.getStatic(match), subtracted.getStatic(match));
  }

  @Override
  public boolean isStatic() {
    return original.isStatic() && subtracted.isStatic();
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
