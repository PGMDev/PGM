package tc.oc.pgm.util.compose;

import java.util.stream.Stream;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

public class Maybe<T> implements Composition<T> {

  private final Filter filter;
  private final Composition<T> element;

  public Maybe(Filter filter, Composition<T> element) {
    this.filter = filter;
    this.element = element;
  }

  @Override
  public Stream<T> elements(Query query) {
    return filter.query(query).isAllowed() ? element.elements(query) : Stream.empty();
  }
}
