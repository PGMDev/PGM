package tc.oc.pgm.util.block;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

/**
 * Hybrid {@link BlockVector} and {@link Iterator} that iterates all block positions in a given
 * cuboid volume. The object changes its own state while iterating, and returns itself from {@link
 * #next()}. In this way, it avoids creating any objects while iterating. Attempts to mutate the
 * object in any way besides the next() method will be punished severely.
 *
 * <p>The boundaries given to the constructor must be in correct order, or the cuboid will be
 * considered empty.
 */
public class CuboidBlockIterator extends ImmutableBlockVector implements Iterator<BlockVector> {

  private final int xMin, yMin;
  private final int xMax, yMax, zMax;
  private int xNext, yNext, zNext;
  private boolean hasNext;

  public CuboidBlockIterator(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
    super(xMin, yMin, zMin);
    this.xNext = this.xMin = xMin;
    this.yNext = this.yMin = yMin;
    this.zNext = zMin;
    this.xMax = xMax;
    this.yMax = yMax;
    this.zMax = zMax;
    this.hasNext = xMin < xMax && yMin < yMax && zMin < zMax;
  }

  public CuboidBlockIterator(Vector min, Vector max) {
    this(
        min.getBlockX(),
        min.getBlockY(),
        min.getBlockZ(),
        max.getBlockX(),
        max.getBlockY(),
        max.getBlockZ());
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public BlockVector next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }

    x = xNext;
    y = yNext;
    z = zNext;

    if (++xNext >= xMax) {
      xNext = xMin;
      if (++yNext >= yMax) {
        yNext = yMin;
        if (++zNext >= zMax) {
          hasNext = false;
        }
      }
    }

    return this;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
