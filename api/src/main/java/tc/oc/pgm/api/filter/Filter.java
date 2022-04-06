package tc.oc.pgm.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.Query;

/**
 * Something that can answer "yes", "no" or "i don't care" to the imagined question: "Can X
 * happen?". To be able to answer the question context is provided initially through a {@link Query}
 * and then processed by the filter itself before a response is given.
 *
 * <p>If a filter is connected to a specific {@link Feature} a {@link FeatureReference} should be
 * passed at xml parse time (when the filter is created) and {@link FeatureReference#get() fetched}
 * at match time
 *
 * @see Query
 */
public interface Filter {

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
        throw new UnsupportedOperationException("Filter did not respond to the query");
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
}
