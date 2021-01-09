package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.match.MatchImpl;

public final class Filterables {
  private Filterables() {}

  // Filterables ordered from general to specific
  public static final List<Class<? extends Filterable<?>>> SCOPES =
      ImmutableList.of(MatchImpl.class, Party.class, PlayerQuery.class);

  /**
   * Return the "scope" of the given filter, which is the most general {@link Filterable} type that
   * it responds to.
   */
  public static Class<? extends Filterable<?>> scope(Filter filter) {
    for (Class<? extends Filterable<?>> scope : SCOPES) {
      if (filter.getQueryType().isAssignableFrom(scope)) return scope;
    }

    throw new IllegalStateException(
        "Filter type "
            + filter.getQueryType().getSimpleName()
            + " does not have a filterable scope");
  }
}
