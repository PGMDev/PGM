package tc.oc.pgm.util.collection;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ForwardingSet;
import java.util.*;

/**
 * A set of objects with a dynamic partial ordering that can be manually invalidated. The ordering
 * is always defined by a comparator passed to the constructor. Iteration is in this order, with
 * equal elements in insertion order.
 *
 * <p>A "rank" is a set of elements that compare equal. Each rank's "position" is the number of
 * non-empty ranks who's elements compare lower than its own elements. The position of the first
 * rank is 0.
 *
 * <p>The elements are lazily sorted and cached whenever a method is called that depends on the
 * ranking order. These methods are {@link #iterator} and {@link #getRank}. The cache is invalidated
 * whenever the collection is changed, or {@link #invalidateRanking} is called.
 */
public class RankedSet<E> extends ForwardingSet<E> {

  private final Comparator<E> comparator;
  private final Set<E> set;
  private final List<E> list = new ArrayList<>();
  private final List<Set<E>> ranks = new ArrayList<>();
  private boolean sorted;

  public RankedSet(Set<E> set, Comparator<E> comparator) {
    this.set = assertNotNull(set);
    this.comparator = assertNotNull(comparator);
  }

  public RankedSet(Comparator<E> comparator) {
    this(new HashSet<E>(), comparator);
  }

  @Override
  protected Set<E> delegate() {
    return set;
  }

  private boolean freshenRanking() {
    if (sorted) return false;

    Collections.sort(list, comparator);

    if (!list.isEmpty()) {
      Set<E> rank = null;
      E last = null;
      for (E e : list) {
        if (last == null || comparator.compare(last, e) != 0) {
          ranks.add(rank = new HashSet<>());
        }
        rank.add(e);
        last = e;
      }
    }

    sorted = true;
    return true;
  }

  public void invalidateRanking() {
    sorted = false;
    ranks.clear();
  }

  /**
   * Return the set of elements with the given position. If no elements have the given position, the
   * empty set is returned.
   */
  public Set<E> getRank(int rank) {
    freshenRanking();
    return rank < ranks.size() ? ranks.get(rank) : Collections.<E>emptySet();
  }

  /** Iterate in ranking order */
  @Override
  public Iterator<E> iterator() {
    freshenRanking();
    return list.iterator();
  }

  @Override
  public boolean add(E e) {
    list.add(e);
    invalidateRanking();
    return super.add(e);
  }

  @Override
  public boolean remove(Object e) {
    list.remove(e);
    invalidateRanking();
    return super.remove(e);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    list.addAll(c);
    invalidateRanking();
    return super.addAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    list.removeAll(c);
    invalidateRanking();
    return super.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    list.retainAll(c);
    invalidateRanking();
    return super.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
    invalidateRanking();
    super.clear();
  }

  @Override
  public <T> T[] toArray(T[] array) {
    Iterator<E> iterator = iterator();
    for (int i = 0; i < array.length && iterator.hasNext(); i++) {
      array[i] = (T) iterator.next();
    }
    return array;
  }

  @Override
  public Object[] toArray() {
    return toArray(new Object[size()]);
  }
}
