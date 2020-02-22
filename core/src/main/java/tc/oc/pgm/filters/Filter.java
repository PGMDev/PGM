package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

public interface Filter {

  /** Least-derived query type that this filter might not abstain from */
  Class<? extends IQuery> getQueryType();

  QueryResponse query(IQuery query);

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
