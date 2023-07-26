package tc.oc.pgm.util;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
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
}
