package tc.oc.pgm.api.filter;

import tc.oc.pgm.api.filter.query.Query;

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
