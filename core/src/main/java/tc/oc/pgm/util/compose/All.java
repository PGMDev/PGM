package tc.oc.pgm.util.compose;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tc.oc.pgm.api.filter.query.Query;

public class All<T> implements Composition<T> {

  private final List<Composition<T>> elements;

  public All(Iterable<? extends Composition<T>> elements) {
    this.elements = ImmutableList.copyOf(elements);
  }

  public All(Stream<? extends Composition<T>> elements) {
    this.elements = elements.collect(Collectors.toList());
  }

  @Override
  public Stream<T> elements(Query query) {
    return elements.stream().flatMap(e -> e.elements(query));
  }
}
