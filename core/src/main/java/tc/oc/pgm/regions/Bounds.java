package tc.oc.pgm.regions;

import java.util.Iterator;
import java.util.Random;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.block.CuboidBlockIterator;

public class Bounds implements Cloneable {
  protected final Vector min;
  protected final Vector max;

  public Bounds(Vector min, Vector max) {
    this.min = min.clone();
    this.max = max.clone();
  }

  /** Create a minimal bounding box containing all of the given points */
  public Bounds(Vector... points) {
    this.min =
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    this.max =
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    for (Vector p : points) {
      this.min.setX(Math.min(this.min.getX(), p.getX()));
      this.min.setY(Math.min(this.min.getY(), p.getY()));
      this.min.setZ(Math.min(this.min.getZ(), p.getZ()));

      this.max.setX(Math.max(this.max.getX(), p.getX()));
      this.max.setY(Math.max(this.max.getY(), p.getY()));
      this.max.setZ(Math.max(this.max.getZ(), p.getZ()));
    }
  }

  public Bounds(Bounds other) {
    this(other.min, other.max);
  }

  @Override
  public Bounds clone() {
    return new Bounds(this);
  }

  public static Bounds unbounded() {
    return new Bounds(
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
  }

  public static Bounds empty() {
    return new Bounds(
        new Vector(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
        new Vector(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
  }

  public static Bounds intersection(Bounds a, Bounds b) {
    if (a.contains(b)) {
      return b;
    } else if (b.contains(a)) {
      return a;
    } else {
      return new Bounds(Vector.getMaximum(a.min, b.min), Vector.getMinimum(a.max, b.max));
    }
  }

  public static boolean disjoint(Bounds a, Bounds b) {
    Vector min = Vector.getMaximum(a.min, b.min);
    Vector max = Vector.getMinimum(a.max, b.max);
    return min.getX() >= max.getX() || min.getY() >= max.getY() || min.getZ() >= max.getZ();
  }

  public static Bounds union(Bounds a, Bounds b) {
    if (a.contains(b)) {
      return a;
    } else if (b.contains(a)) {
      return b;
    } else {
      return new Bounds(Vector.getMinimum(a.min, b.min), Vector.getMaximum(a.max, b.max));
    }
  }

  public static Bounds complement(Bounds wrt, Bounds of) {
    // The booleans reflect if the subtracted set contains the
    // original set on each axis. The final bounds for each axis
    // are then the minimum of the two sets, if the other two axes
    // are containing, otherwise the bounds of the original set.
    boolean cx = of.min.getX() < wrt.min.getX() && of.max.getX() > wrt.max.getX();
    boolean cy = of.min.getY() < wrt.min.getY() && of.max.getY() > wrt.max.getY();
    boolean cz = of.min.getZ() < wrt.min.getZ() && of.max.getZ() > wrt.max.getZ();
    return new Bounds(
        new Vector(
            cy && cz ? Math.max(wrt.min.getX(), of.min.getX()) : wrt.min.getX(),
            cz && cx ? Math.max(wrt.min.getY(), of.min.getY()) : wrt.min.getY(),
            cx && cy ? Math.max(wrt.min.getZ(), of.min.getZ()) : wrt.min.getZ()),
        new Vector(
            cy && cz ? Math.min(wrt.max.getX(), of.max.getX()) : wrt.max.getX(),
            cz && cx ? Math.min(wrt.max.getY(), of.max.getY()) : wrt.max.getY(),
            cx && cy ? Math.min(wrt.max.getZ(), of.max.getZ()) : wrt.max.getZ()));
  }

  public Bounds translate(Vector offset) {
    Bounds translated = new Bounds(this);
    translated.min.add(offset);
    translated.max.add(offset);
    return translated;
  }

  public boolean isFinite() {
    return this.isEmpty()
        || !(Double.isInfinite(this.min.getX())
            || Double.isInfinite(this.max.getX())
            || Double.isInfinite(this.min.getY())
            || Double.isInfinite(this.max.getY())
            || Double.isInfinite(this.min.getZ())
            || Double.isInfinite(this.max.getZ()));
  }

  public boolean isBlockFinite() {
    return this.isEmpty()
        || !(Double.isInfinite(this.min.getX())
            || Double.isInfinite(this.max.getX())
            || Double.isInfinite(this.min.getZ())
            || Double.isInfinite(this.max.getZ()));
  }

  public boolean isEmpty() {
    return min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ();
  }

  public boolean contains(Vector point) {
    return point.isInAABB(this.min, this.max);
  }

  public boolean contains(Bounds bounds) {
    return this.contains(bounds.min) && this.contains(bounds.max);
  }

  public Vector getMin() {
    return this.min.clone();
  }

  public Vector getMax() {
    return this.max.clone();
  }

  public Vector getSize() {
    return this.max.clone().subtract(this.min);
  }

  public double getVolume() {
    Vector size = this.getSize();
    return size.getX() * size.getY() * size.getZ();
  }

  public Vector getCenterPoint() {
    return this.min.clone().add(this.max).multiply(0.5);
  }

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

  public Vector getRandomPoint(Random random) {
    Vector size = this.getSize();
    return new Vector(
        this.min.getX() + size.getX() * random.nextDouble(),
        this.min.getY() + size.getY() * random.nextDouble(),
        this.min.getZ() + size.getZ() * random.nextDouble());
  }

  public BlockVector getBlockMin() {
    return new BlockVector(
        (int) this.min.getX() + 0.5d,
        (int) Math.max(0, Math.min(255, this.min.getY() + 0.5d)),
        (int) this.min.getZ() + 0.5d);
  }

  public BlockVector getBlockMaxInside() {
    return new BlockVector(
        (int) this.max.getX() - 0.5d,
        (int) Math.max(0, Math.min(255, this.max.getY() - 0.5d)),
        (int) this.max.getZ() - 0.5d);
  }

  public BlockVector getBlockMaxOutside() {
    return new BlockVector(
        (int) this.max.getX() + 0.5d,
        (int) Math.max(0, Math.min(256, this.max.getY() + 0.5d)),
        (int) this.max.getZ() + 0.5d);
  }

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

  public BlockVector getBlockSize() {
    return this.getBlockMaxOutside().subtract(this.getBlockMin()).toBlockVector();
  }

  public int getBlockVolume() {
    BlockVector size = this.getBlockSize();
    return (int) (size.getX() * size.getY() * size.getZ());
  }

  public BlockVector getRandomBlock(Random random) {
    BlockVector min = this.getBlockMin();
    BlockVector size = this.getBlockSize();
    return new BlockVector(
        min.getX() + random.nextInt(size.getBlockX()),
        min.getY() + random.nextInt(size.getBlockY()),
        min.getZ() + random.nextInt(size.getBlockZ()));
  }

  /** Iterate over all the block locations within these bounds, in Z-major order. */
  public Iterator<BlockVector> getBlockIterator() {
    if (!this.isBlockFinite()) {
      throw new UnsupportedOperationException("Cannot get all blocks from an infinite region");
    }
    return new CuboidBlockIterator(getBlockMin(), getBlockMaxOutside());
  }

  public Iterable<BlockVector> getBlocks() {
    return this::getBlockIterator;
  }

  @Override
  public String toString() {
    return "Bounds{min=[" + this.min.toString() + "],max=[" + this.max.toString() + "]}";
  }
}
