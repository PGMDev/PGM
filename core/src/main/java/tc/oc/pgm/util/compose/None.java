package tc.oc.pgm.util.compose;

import java.util.stream.Stream;
import tc.oc.pgm.api.filter.query.Query;

public class None<T> implements Composition<T> {

  @Override
  public Stream<T> elements(Query query) {
    return Stream.empty();
  }
}
