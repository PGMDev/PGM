package tc.oc.pgm.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

  public static <T> Stream<T> of(Iterable<T> iterable) {
    return of(iterable.iterator());
  }

  public static <T> Stream<T> of(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }
}
