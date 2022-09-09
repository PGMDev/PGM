package tc.oc.pgm.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.parse.FilterParser;

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
public interface Filter extends FeatureDefinition {

  /** ALLOW or DENY the given {@link Query}, or ABSTAIN from responding. */
  QueryResponse query(Query query);

  /**
   * Return true if this filter ALLOWs the given {@link Query}, false if this filter DENYes it, or
   * throw a {@link UnsupportedOperationException} if this filter cannot respond to the given query.
   *
   * <p>{@link #respondsTo(Class)} can be used to ensure that this method will not throw.
   */
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

  /**
   * Return true only if ALL following conditions are true:
   *
   * <p>1. This filter always responds to queries of the given type (i.e. never ABSTAINS) 2. This
   * filter's response is derived only from properties of the given query type, and not on any
   * properties in subtypes of that query. 3. All dependencies of this filter also meet the above
   * conditions.
   */
  boolean respondsTo(Class<? extends Query> queryType);

  /**
   * Does this filter support dynamic notifications?
   *
   * <p>If this returns true, then any change in the response of this filter to a query that passes
   * {@link #respondsTo(Class)} must notify {@link FilterListener}s registered through {@link
   * tc.oc.pgm.filters.FilterMatchModule}.
   *
   * <p>This method should NOT account for the behavior of any {@link #dependencies()}, as that is
   * done automatically by the calling code. This method can return true as long as it does NOT
   * change its response to any query, without firing a notification, at a time when none of its
   * dynamic dependencies change their response.
   */
  default boolean isDynamic() {
    return !getRelevantEvents().isEmpty();
  }

  /**
   * Filters with children are responsible for returning the events of their children.
   *
   * <p>Empty list tells us that the filter is not dynamic
   */
  default Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of();
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

    public boolean isPresent() {
      return this != ABSTAIN;
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
