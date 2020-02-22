package tc.oc.util.collection;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import java.util.*;
import javax.annotation.Nullable;

/** {@link Iterable}-related utilities. */
public class IterableUtils {
  /**
   * Finds the most common element in the specified {@link Iterable}. If there is a tie and <code>
   * tie</code> is true, a random selection (from the tied elements) is made. If there is a tie and
   * <code>tie</code> is not true, <code>null</code> is returned, instead.
   *
   * @param iterable The {@link Iterable} to check.
   * @param <T> The type of the {@link Iterable}.
   * @return The most common element, an arbitrary selection from the tied elements if a tie arises
   *     and <code>tie</code> is true, or <code>null</code> if a tie arises and <code>tie</code> is
   *     not true..
   */
  public static @Nullable <T> T findMostCommon(Iterable<T> iterable, boolean tie) {
    HashMap<T, Integer> counts = new HashMap<>();
    for (T obj : Preconditions.checkNotNull(iterable, "Iterable")) {
      Integer count = counts.get(obj);
      counts.put(obj, count == null ? 1 : count + 1);
    }

    int max = 0;
    T maxObj = null;
    for (Map.Entry<T, Integer> entry : counts.entrySet()) {
      int value = entry.getValue();
      if (maxObj == null || value >= max) {
        max = value;
        maxObj = entry.getKey();
      }
    }

    if (!tie && maxObj != null) {
      for (Map.Entry<T, Integer> entry : counts.entrySet()) {
        if (entry.getValue() == max && !maxObj.equals(entry.getKey())) {
          return null;
        }
      }
    }

    return maxObj;
  }

  /**
   * Finds the most common element in the specified {@link Iterable}. If there is a tie, a random
   * selection (from the tied elements) is made. If this functionality is undesired, {@link
   * #findMostCommon(Iterable, boolean)} may be used instead.
   *
   * @param iterable The {@link Iterable} to check.
   * @param <T> The type of the {@link Iterable}.
   * @return The most common element, or an arbitrary selection from the tied elements if a tie
   *     arises.
   */
  public static <T> T findMostCommon(Iterable<T> iterable) {
    return findMostCommon(iterable, true);
  }

  /**
   * Transform and filter at the same time. Return null from the transform function to skip the
   * current element.
   */
  public static <In, Out> Iterator<Out> transfilter(
      final Iterator<In> iterator, final Function<? super In, ? extends Out> function) {
    return new Iterator<Out>() {
      Out next;

      @Override
      public boolean hasNext() {
        if (next != null) {
          return true;
        } else {
          while (iterator.hasNext()) {
            next = function.apply(iterator.next());
            if (next != null) return true;
          }
          return false;
        }
      }

      @Override
      public Out next() {
        if (!hasNext()) throw new NoSuchElementException();
        Out tmp = next;
        next = null;
        return tmp;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Transform and filter at the same time. Return null from the transform function to skip the
   * current element.
   */
  public static <In, Out> Iterable<Out> transfilter(
      final Iterable<In> iterable, final Function<? super In, ? extends Out> function) {
    return new Iterable<Out>() {
      @Override
      public Iterator<Out> iterator() {
        return transfilter(iterable.iterator(), function);
      }
    };
  }

  public static <In, Out> Collection<Out> transfilter(
      final Collection<In> collection, final Function<? super In, ? extends Out> function) {
    return ImmutableList.copyOf(transfilter((Iterable) collection, function));
  }

  public static Iterable<String> toStrings(Iterable<?> things) {
    return Iterables.transform(
        things,
        new Function<Object, String>() {
          @Override
          public String apply(@Nullable Object input) {
            return String.valueOf(input);
          }
        });
  }

  public static Collection<String> toStrings(Collection<?> things) {
    return Collections2.transform(
        things,
        new Function<Object, String>() {
          @Override
          public String apply(@Nullable Object input) {
            return String.valueOf(input);
          }
        });
  }

  /**
   * Return a copy of the given collection in whatever subclass of {@link ImmutableCollection} fits
   * best
   */
  public static <E> ImmutableCollection<E> immutableCopyOf(Collection<E> things) {
    if (things instanceof List) {
      return ImmutableList.copyOf(things);
    } else {
      return ImmutableSet.copyOf(things);
    }
  }
}
