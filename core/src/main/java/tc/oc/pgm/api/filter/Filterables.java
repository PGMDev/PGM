package tc.oc.pgm.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

public interface Filterables {
  /** {@link Filterable}s ordered from general to specific */
  List<Class<? extends Filterable<?>>> SCOPES =
      ImmutableList.of(Match.class, Party.class, MatchPlayer.class);

  /**
   * Return the "scope" of the given filter, which is the most general {@link Filterable} type that
   * it responds to.
   */
  static Class<? extends Filterable<?>> scope(Filter filter) {
    for (Class<? extends Filterable<?>> scope : SCOPES) {
      if (filter.respondsTo(scope)) return scope;
    }

    throw new IllegalStateException(
        "Filter type " + filter.getClass().getSimpleName() + " does not have a filterable scope");
  }
}
