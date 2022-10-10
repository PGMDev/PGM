package tc.oc.pgm.util.block;

import gnu.trove.TLongCollection;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
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

  private final TLongSet set;

  public BlockVectorSet(TLongSet set) {
    this.set = set;
  }

  public BlockVectorSet(int capacity) {
    this(new TLongHashSet(capacity));
  }

  public BlockVectorSet(Collection<? extends BlockVector> that) {
    this(that.size());
    addAll(that);
  }

  public BlockVectorSet() {
    this(Constants.DEFAULT_CAPACITY);
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
    final TLongIterator iter = this.set.iterator();

    return new Iterator<BlockVector>() {
      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public BlockVector next() {
        return BlockVectors.decodePos(iter.next());
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

  public boolean containsAll(long[] encoded) {
    return this.set.containsAll(encoded);
  }

  public boolean containsAll(TLongCollection encoded) {
    return this.set.containsAll(encoded);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!this.contains(o)) return false;
    }
    return true;
  }

  public boolean addAll(long[] encoded) {
    return this.set.addAll(encoded);
  }

  public boolean addAll(TLongCollection encoded) {
    return this.set.addAll(encoded);
  }

  @Override
  public boolean addAll(Collection<? extends BlockVector> vectors) {
    for (BlockVector v : vectors) {
      this.add(v);
    }
    return false;
  }

  public boolean retainAll(long[] encoded) {
    return this.set.retainAll(encoded);
  }

  public boolean retainAll(TLongCollection encoded) {
    return this.set.retainAll(encoded);
  }

  @Override
  public boolean retainAll(Collection<?> vectors) {
    return this.retainAll(BlockVectors.encodePosSet(vectors));
  }

  public boolean removeAll(TLongCollection encoded) {
    return set.removeAll(encoded);
  }

  public boolean removeAll(long[] encoded) {
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

  public long getEncodedAt(int n) {
    return set.toArray()[n];
  }

  /**
   * Get the element at position N for some arbitrary ordering. The order is constant as long as the
   * state of the container does not change, but is otherwise undefined.
   */
  public BlockVector getAt(int n) {
    return BlockVectors.decodePos(getEncodedAt(n));
  }

  public BlockVector chooseRandom(Random random) {
    // The Trove set uses a sparse array, so there isn't really any
    // faster way to do this, not even by messing with Trove internals.
    final TLongIterator iterator = set.iterator();
    long encoded = 0;
    for (int n = random.nextInt(size()); n >= 0; n--) {
      encoded = iterator.next();
    }
    return BlockVectors.decodePos(encoded);
  }
}
