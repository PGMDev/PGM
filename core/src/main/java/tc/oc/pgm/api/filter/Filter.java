package tc.oc.pgm.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.dynamic.Filterable;

/**
 * Something that can answer "yes", "no" or "i don't care" to the imagined question: "Can X
 * happen?". To be able to answer the question context is provided initially through a {@link Query}
 * and then processed by the filter itself before a response is given.
 *
 * <p>If a filter is connected to a specific {@link Feature} a {@link FeatureReference} should be
 * passed at xml parse time (when the filter is created) and {@link FeatureReference#get() fetched}
 * at match time
 *
 * <p>For examples on how to create filters see {@link FilterParser}
 *
 * @see Query
 */
public interface Filter {

  /** {@link Filterable}s ordered from general to specific */
  List<Class<? extends Filterable<?>>> SCOPES =
      ImmutableList.of(Match.class, Party.class, MatchPlayer.class);

  /**
   * Filters with children are responsible for returning the events of their children.
   *
   * <p>Empty list tells us that the filter is not dynamic
   */
  default Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of();
  }

  /** Least-derived query type that this filter might not abstain from */
  Class<? extends Query> getQueryType();

  QueryResponse query(Query query);

  default boolean response(Query query) {
    switch (query(query)) {
      case ALLOW:
        return true;
      case DENY:
        return false;
      default:
        throw new UnsupportedOperationException(
            "Filter " + this + " did not respond to the query " + query);
    }
  }

  enum QueryResponse {
    ALLOW,
    DENY,
    ABSTAIN;

    public boolean isAllowed() {
      return this == ALLOW || this == ABSTAIN;
    }

    public boolean isDenied() {
      return this == DENY;
    }

    public static QueryResponse any(QueryResponse... responses) {
      QueryResponse result = ABSTAIN;
      for (QueryResponse response : responses) {
        switch (response) {
          case ALLOW:
            return ALLOW;
          case DENY:
            result = DENY;
            break;
        }
      }
      return result;
    }

    public static QueryResponse all(QueryResponse... responses) {
      QueryResponse result = ABSTAIN;
      for (QueryResponse response : responses) {
        switch (response) {
          case DENY:
            return DENY;
          case ALLOW:
            result = ALLOW;
            break;
        }
      }
      return result;
    }

    public static QueryResponse first(QueryResponse... responses) {
      for (QueryResponse response : responses) {
        if (response != ABSTAIN) return response;
      }
      return ABSTAIN;
    }

    public static QueryResponse fromBoolean(boolean allow) {
      return allow ? ALLOW : DENY;
    }
  }

  /**
   * Return the "scope" of this filter, which is the most general {@link Filterable} type that it
   * responds to.
   */
  default Class<? extends Filterable<?>> getScope() {
    for (Class<? extends Filterable<?>> scope : SCOPES) {
      if (this.getQueryType().isAssignableFrom(scope)) return scope;
    }

    throw new IllegalStateException(
        "Filter type " + this.getQueryType().getSimpleName() + " does not have a filterable scope");
  }
}
