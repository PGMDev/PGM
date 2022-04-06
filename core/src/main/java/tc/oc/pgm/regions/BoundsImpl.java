package tc.oc.pgm.regions;

import java.util.Iterator;
import java.util.Random;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Bounds;
import tc.oc.pgm.util.block.CuboidBlockIterator;

public class BoundsImpl implements Cloneable, Bounds {
  protected final Vector min;
  protected final Vector max;

  public BoundsImpl(Vector min, Vector max) {
    this.min = min.clone();
    this.max = max.clone();
  }

  /** Create a minimal bounding box containing all of the given points */
  public BoundsImpl(Vector... points) {
    this.min =
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    this.max =
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    for (Vector p : points) {
      this.min.setX(Math.min(this.min.getX(), p.getX()));
      this.min.setY(Math.min(this.min.getY(), p.getY()));
      this.min.setZ(Math.min(this.min.getZ(), p.getZ()));

      this.getMax().setX(Math.max(this.getMax().getX(), p.getX()));
      this.getMax().setY(Math.max(this.getMax().getY(), p.getY()));
      this.getMax().setZ(Math.max(this.getMax().getZ(), p.getZ()));
    }
  }

  public BoundsImpl(BoundsImpl other) {
    this(other.min, other.max);
  }

  public static BoundsImpl unbounded() {
    return new BoundsImpl(
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
  }

  public static BoundsImpl empty() {
    return new BoundsImpl(
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
  }

  public static Bounds intersection(Bounds a, Bounds b) {
    if (a.contains(b)) {
      return b;
    } else if (b.contains(a)) {
      return a;
    } else {
      return new BoundsImpl(
          Vector.getMaximum(a.getMin(), b.getMin()), Vector.getMinimum(a.getMax(), b.getMax()));
    }
  }

  public static Bounds union(Bounds a, Bounds b) {
    if (a.contains(b)) {
      return a;
    } else if (b.contains(a)) {
      return b;
    } else {
      return new BoundsImpl(
          Vector.getMinimum(a.getMin(), b.getMin()), Vector.getMaximum(a.getMax(), b.getMax()));
    }
  }

  public static BoundsImpl complement(Bounds wrt, Bounds of) {
    // The booleans reflect if the subtracted set contains the
    // original set on each axis. The final bounds for each axis
    // are then the minimum of the two sets, if the other two axes
    // are containing, otherwise the bounds of the original set.
    boolean cx =
        of.getMin().getX() < wrt.getMin().getX() && of.getMax().getX() > wrt.getMax().getX();
    boolean cy =
        of.getMin().getY() < wrt.getMin().getY() && of.getMax().getY() > wrt.getMax().getY();
    boolean cz =
        of.getMin().getZ() < wrt.getMin().getZ() && of.getMax().getZ() > wrt.getMax().getZ();
    return new BoundsImpl(
        new Vector(
            cy && cz ? Math.max(wrt.getMin().getX(), of.getMin().getX()) : wrt.getMin().getX(),
            cz && cx ? Math.max(wrt.getMin().getY(), of.getMin().getY()) : wrt.getMin().getY(),
            cx && cy ? Math.max(wrt.getMin().getZ(), of.getMin().getZ()) : wrt.getMin().getZ()),
        new Vector(
            cy && cz ? Math.min(wrt.getMax().getX(), of.getMax().getX()) : wrt.getMax().getX(),
            cz && cx ? Math.min(wrt.getMax().getY(), of.getMax().getY()) : wrt.getMax().getY(),
            cx && cy ? Math.min(wrt.getMax().getZ(), of.getMax().getZ()) : wrt.getMax().getZ()));
  }

  @Override
  public BoundsImpl clone() {
    return new BoundsImpl(this);
  }

  @Override
  public Bounds translate(Vector offset) {
    BoundsImpl translated = new BoundsImpl(this);
    translated.min.add(offset);
    translated.max.add(offset);
    return translated;
  }

  @Override
  public boolean isFinite() {
    return this.isEmpty()
        || !(Double.isInfinite(this.min.getX())
            || Double.isInfinite(this.max.getX())
            || Double.isInfinite(this.min.getY())
            || Double.isInfinite(this.max.getY())
            || Double.isInfinite(this.min.getZ())
            || Double.isInfinite(this.max.getZ()));
  }

  @Override
  public boolean isBlockFinite() {
    return this.isEmpty()
        || !(Double.isInfinite(this.min.getX())
            || Double.isInfinite(this.max.getX())
            || Double.isInfinite(this.min.getZ())
            || Double.isInfinite(this.max.getZ()));
  }

  @Override
  public boolean isEmpty() {
    return min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ();
  }

  @Override
  public boolean contains(Vector point) {
    return point.isInAABB(this.min, this.max);
  }

  @Override
  public boolean contains(Bounds bounds) {
    return this.contains(bounds.getMin()) && this.contains(bounds.getMax());
  }

  @Override
  public Vector getMin() {
    return this.min.clone();
  }

  @Override
  public Vector getMax() {
    return this.max.clone();
  }

  @Override
  public Vector getSize() {
    return this.max.clone().subtract(this.min);
  }

  @Override
  public double getVolume() {
    Vector size = this.getSize();
    return size.getX() * size.getY() * size.getZ();
  }

  @Override
  public Vector getCenterPoint() {
    return this.min.clone().add(this.max).multiply(0.5);
  }

  @Override
  public Vector[] getVertices() {
    return new Vector[] {
      this.min.clone(),
      new Vector(this.min.getX(), this.min.getY(), this.max.getZ()),
      new Vector(this.min.getX(), this.max.getY(), this.min.getZ()),
      new Vector(this.min.getX(), this.max.getY(), this.max.getZ()),
      new Vector(this.max.getX(), this.min.getY(), this.min.getZ()),
      new Vector(this.max.getX(), this.min.getY(), this.max.getZ()),
      new Vector(this.max.getX(), this.max.getY(), this.min.getZ()),
      this.max.clone()
    };
  }

  @Override
  public Vector getRandomPoint(Random random) {
    Vector size = this.getSize();
    return new Vector(
        this.min.getX() + size.getX() * random.nextDouble(),
        this.min.getY() + size.getY() * random.nextDouble(),
        this.min.getZ() + size.getZ() * random.nextDouble());
  }

  @Override
  public BlockVector getBlockMin() {
    return new BlockVector(
        (int) this.min.getX() + 0.5d,
        (int) Math.max(0, Math.min(255, this.min.getY() + 0.5d)),
        (int) this.min.getZ() + 0.5d);
  }

  @Override
  public BlockVector getBlockMaxInside() {
    return new BlockVector(
        (int) this.max.getX() - 0.5d,
        (int) Math.max(0, Math.min(255, this.max.getY() - 0.5d)),
        (int) this.max.getZ() - 0.5d);
  }

  @Override
  public BlockVector getBlockMaxOutside() {
    return new BlockVector(
        (int) this.max.getX() + 0.5d,
        (int) Math.max(0, Math.min(256, this.max.getY() + 0.5d)),
        (int) this.max.getZ() + 0.5d);
  }

  @Override
  public boolean containsBlock(BlockVector v) {
    BlockVector min = this.getBlockMin();
    BlockVector max = this.getBlockMaxInside();
    return min.getBlockX() <= v.getBlockX()
        && v.getBlockX() <= max.getBlockX()
        && min.getBlockY() <= v.getBlockY()
        && v.getBlockY() <= max.getBlockY()
        && min.getBlockZ() <= v.getBlockZ()
        && v.getBlockZ() <= max.getBlockZ();
  }

  @Override
  public BlockVector getBlockSize() {
    return this.getBlockMaxOutside().subtract(this.getBlockMin()).toBlockVector();
  }

  @Override
  public int getBlockVolume() {
    BlockVector size = this.getBlockSize();
    return (int) (size.getX() * size.getY() * size.getZ());
  }

  @Override
  public BlockVector getRandomBlock(Random random) {
    BlockVector min = this.getBlockMin();
    BlockVector size = this.getBlockSize();
    return new BlockVector(
        min.getX() + random.nextInt(size.getBlockX()),
        min.getY() + random.nextInt(size.getBlockY()),
        min.getZ() + random.nextInt(size.getBlockZ()));
  }

  /** Iterate over all the block locations within these bounds, in Z-major order. */
  @Override
  public Iterator<BlockVector> getBlockIterator() {
    if (!this.isBlockFinite()) {
      throw new UnsupportedOperationException("Cannot get all blocks from an infinite region");
    }
    return new CuboidBlockIterator(getBlockMin(), getBlockMaxOutside());
  }

  @Override
  public Iterable<BlockVector> getBlocks() {
    return new Iterable<BlockVector>() {
      @Override
      public Iterator<BlockVector> iterator() {
        return getBlockIterator();
      }
    };
  }

  @Override
  public String toString() {
    return "Bounds{min=[" + this.min.toString() + "],max=[" + this.max.toString() + "]}";
  }
}
