package tc.oc.pgm.api.region;

import java.util.Iterator;
import java.util.Random;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public interface Bounds {

  Bounds translate(Vector offset);

  boolean isFinite();

  boolean isBlockFinite();

  boolean isEmpty();

  boolean contains(Vector point);

  boolean contains(Bounds bounds);

  Vector getMin();

  Vector getMax();

  Vector getSize();

  double getVolume();

  Vector getCenterPoint();

  Vector[] getVertices();

  Vector getRandomPoint(Random random);

  BlockVector getBlockMin();

  BlockVector getBlockMaxInside();

  BlockVector getBlockMaxOutside();

  boolean containsBlock(BlockVector v);

  BlockVector getBlockSize();

  int getBlockVolume();

  BlockVector getRandomBlock(Random random);

  Iterator<BlockVector> getBlockIterator();

  Iterable<BlockVector> getBlocks();

  Bounds clone();
}
