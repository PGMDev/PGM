package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.math.TransformMatrix;

public class ResizedRegion extends TransformedRegion {
  private final Vector min, max;
  private final boolean relative;
  private TransformMatrix matrix;
  private TransformMatrix inverse;

  public ResizedRegion(Region region, Vector min, Vector max, boolean relative) {
    super(region);
    this.min = min;
    this.max = max;
    this.relative = relative;
  }

  @Override
  protected Vector transform(Vector point) {
    if (matrix == null) ensureInitialized();
    return matrix.transform(point);
  }

  @Override
  protected Vector untransform(Vector point) {
    if (inverse == null) ensureInitialized();
    return inverse.transform(point);
  }

  @Override
  public Bounds getBounds() {
    ensureInitialized();
    return super.getBounds();
  }

  private void ensureInitialized() {
    if (matrix != null) return;

    var oldBounds = region.getBounds();
    if (oldBounds.isEmpty() || !oldBounds.isBlockFinite()) {
      this.bounds = oldBounds;
      this.matrix = this.inverse = TransformMatrix.identity();
      return;
    }

    var oldSize = oldBounds.getSize();

    if (relative) {
      min.multiply(oldSize);
      max.multiply(oldSize);
    }

    this.bounds =
        new Bounds(oldBounds.getMin().subtract(min), oldBounds.getMax().add(max));
    var newSize = bounds.getSize();

    this.matrix = TransformMatrix.concat(
        TransformMatrix.untranslate(oldBounds.getMin()),
        TransformMatrix.scale(newSize.clone().divide(oldSize)),
        TransformMatrix.translate(bounds.getMin()));

    this.inverse = TransformMatrix.concat(
        TransformMatrix.untranslate(bounds.getMin()),
        TransformMatrix.scale(oldSize.clone().divide(newSize)),
        TransformMatrix.translate(oldBounds.getMin()));
  }
}
