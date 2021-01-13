package tc.oc.pgm.regions;

import java.util.Random;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.region.Region;

public abstract class TransformedRegion extends AbstractRegion {

  protected final Region region;
  protected @Nullable Bounds bounds;

  public TransformedRegion(Region region) {
    this.region = region;
  }

  @Override
  public boolean isBlockBounded() {
    return this.region.isBlockBounded();
  }

  @Override
  public boolean isEmpty() {
    return this.region.isEmpty();
  }

  @Override
  public Bounds getBounds() {
    if (this.bounds == null) {
      this.bounds = this.getTransformedBounds();
    }
    return this.bounds;
  }

  /**
   * Generic bounding box transform - transform all 8 vertices and find the minimum bounding box
   * containing them.
   */
  protected Bounds getTransformedBounds() {
    Vector[] oldVertices = this.region.getBounds().getVertices();
    Vector[] newVertices = new Vector[8];
    for (int i = 0; i < oldVertices.length; i++) {
      newVertices[i] = this.transform(oldVertices[i]);
    }
    return new Bounds(newVertices);
  }

  @Override
  public boolean contains(Vector point) {
    return this.region.contains(this.untransform(point));
  }

  @Override
  public boolean canGetRandom() {
    return this.region.canGetRandom();
  }

  /**
   * Generic random point generator that simply transforms a point generated by the transformed
   * region. This will work at least with affine transformations, but other types may make the
   * random distribution uneven.
   */
  @Override
  public Vector getRandom(Random random) {
    return this.transform(this.region.getRandom(random));
  }

  protected abstract Vector transform(Vector point);

  protected abstract Vector untransform(Vector point);
}
