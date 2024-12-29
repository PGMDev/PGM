package tc.oc.pgm.util.block;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.bukkit.util.BlockVector;

/**
 * Optimized implementation of a set of block locations. Coordinates are encoded into a single long
 * integer with bit masking, and stored in a Trove primitive collection.
 */
public class BlockVectorSet implements Set<BlockVector> {

  private final LongSet set;

  public BlockVectorSet(LongSet set) {
    this.set = set;
  }

  public BlockVectorSet(int capacity) {
    this(new LongOpenHashSet(capacity));
  }

  public BlockVectorSet(Collection<? extends BlockVector> that) {
    this(that.size());
    addAll(that);
  }

  public BlockVectorSet() {
    this(LongOpenHashSet.DEFAULT_INITIAL_SIZE);
  }

  public static BlockVectorSet of(Collection<? extends BlockVector> that) {
    return that instanceof BlockVectorSet ? (BlockVectorSet) that : new BlockVectorSet(that);
  }

  @Override
  public int size() {
    return this.set.size();
  }

  @Override
  public boolean isEmpty() {
    return this.set.isEmpty();
  }

  @Override
  public Iterator<BlockVector> iterator() {
    final var iter = this.set.iterator();

    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public BlockVector next() {
        return BlockVectors.decodePos(iter.nextLong());
      }

      @Override
      public void remove() {
        iter.remove();
      }
    };
  }

  public boolean contains(long encoded) {
    return this.set.contains(encoded);
  }

  public boolean contains(int x, int y, int z) {
    return this.contains(BlockVectors.encodePos(x, y, z));
  }

  @Override
  public boolean contains(Object o) {
    return o instanceof BlockVector && this.set.contains(BlockVectors.encodePos((BlockVector) o));
  }

  public boolean add(long encoded) {
    return this.set.add(encoded);
  }

  public boolean add(int x, int y, int z) {
    return this.add(BlockVectors.encodePos(x, y, z));
  }

  @Override
  public boolean add(BlockVector vector) {
    return this.add(BlockVectors.encodePos(vector));
  }

  public boolean remove(long encoded) {
    return this.set.remove(encoded);
  }

  public boolean remove(int x, int y, int z) {
    return this.remove(BlockVectors.encodePos(x, y, z));
  }

  @Override
  public boolean remove(Object o) {
    return o instanceof BlockVector && this.remove(BlockVectors.encodePos((BlockVector) o));
  }

  public boolean containsAll(LongCollection encoded) {
    return this.set.containsAll(encoded);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!this.contains(o)) return false;
    }
    return true;
  }

  public boolean addAll(LongCollection encoded) {
    return this.set.addAll(encoded);
  }

  @Override
  public boolean addAll(Collection<? extends BlockVector> vectors) {
    for (BlockVector v : vectors) {
      this.add(v);
    }
    return false;
  }

  public boolean retainAll(LongCollection encoded) {
    return this.set.retainAll(encoded);
  }

  @Override
  public boolean retainAll(Collection<?> vectors) {
    return this.retainAll(BlockVectors.encodePosSet(vectors));
  }

  public boolean removeAll(LongCollection encoded) {
    return set.removeAll(encoded);
  }

  @Override
  public boolean removeAll(Collection<?> vectors) {
    return this.removeAll(BlockVectors.encodePosSet(vectors));
  }

  @Override
  public void clear() {
    this.set.clear();
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  public BlockVector chooseRandom(Random random) {
    // The FastUtil set uses a sparse array, so there isn't really any
    // faster way to do this, not even by messing with FastUtil internals.
    final LongIterator iterator = set.iterator();
    long encoded = 0;
    for (int n = random.nextInt(size()); n >= 0; n--) {
      encoded = iterator.nextLong();
    }
    return BlockVectors.decodePos(encoded);
  }
}
