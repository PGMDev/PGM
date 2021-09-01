package tc.oc.pgm.api.filter;

import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.FilterParser;

/**
 * Something that can answers "yes", "no" or "i don't care" to the imagined question: "Can X
 * happen?". To be able to answer the question context is needed though, which is provided initially
 * by a {@link Query} and then processed by the filter itself before a response is given.
 *
 * <p>If a filter is connected to a specific {@link Feature} a {@link FeatureReference} should be
 * passed at xml parse time(when the filter is created) and {@link FeatureReference#get() fetched}
 * at match time
 *
 * <p>For examples on how to create filters see {@link FilterParser}
 *
 * @see Query
 */
public interface Filter {

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
