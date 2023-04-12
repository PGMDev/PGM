package tc.oc.pgm.util.compose;

import java.util.stream.Stream;
import tc.oc.pgm.api.filter.query.Query;

/**
 * A structure of operators that generate a flat sequence of {@link T}s from a {@link Query}.
 *
 * <p>Different queries will generate different sequences, within the rules of the operators.
 */
public interface Composition<T> {

  Stream<T> elements(Query query);
}
