package tc.oc.pgm.util.compose;

import java.util.stream.Stream;
import tc.oc.pgm.api.filter.query.Query;

/** Singleton */
public class Unit<T> implements Composition<T> {

  private final T element;

  public Unit(T element) {
    this.element = element;
  }

  @Override
  public Stream<T> elements(Query query) {
    return Stream.of(element);
  }
}
