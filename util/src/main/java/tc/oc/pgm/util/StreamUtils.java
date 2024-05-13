package tc.oc.pgm.util;

import com.google.common.collect.ImmutableList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

  private static final Collector<Object, ?, ImmutableList<Object>> TO_IMMUTABLE_LIST =
      Collector.of(
          ImmutableList::builder,
          ImmutableList.Builder::add,
          (ImmutableList.Builder<Object> a, ImmutableList.Builder<Object> b) -> {
            a.addAll(b.build());
            return a;
          },
          ImmutableList.Builder::build);

  public static <T> Stream<T> of(Iterable<T> iterable) {
    return of(iterable.iterator());
  }

  public static <T> Stream<T> of(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return (Collector) TO_IMMUTABLE_LIST;
  }

  public static <T> Stream<T> toStream(Enumeration<T> e) {
    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
          public boolean tryAdvance(Consumer<? super T> action) {
            if (e.hasMoreElements()) {
              action.accept(e.nextElement());
              return true;
            }
            return false;
          }

          public void forEachRemaining(Consumer<? super T> action) {
            while (e.hasMoreElements()) action.accept(e.nextElement());
          }
        },
        false);
  }
}
