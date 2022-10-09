package tc.oc.pgm.filters.modifier;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.WeakTypedFilter;

/**
 * Takes a query and modifies it before passing it onto its child filter. Might transform query type
 * so {@link #queryType()} on this modifier can be different from its child filter
 *
 * <p>If the modified query returns {@code null} this modifier will abstain without querying the
 * child filter.
 *
 * @param <Q> is the type of query this filter can modify before passing it on to its child filter
 */
public abstract class QueryModifier<Q extends Query, R extends Query>
    implements WeakTypedFilter<Q> {

  private final Filter filter;
  private final Class<R> innerQueryType;

  protected QueryModifier(Filter filter, Class<R> innerQueryType) {
    this.filter = assertNotNull(filter, "filter may not be null");
    this.innerQueryType = innerQueryType;
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    return queryType().isAssignableFrom(queryType) && filter.respondsTo(innerQueryType);
  }

  public abstract Class<? extends Q> queryType();

  public QueryResponse queryTyped(Q query) {
    Query modifiedQuery = transformQuery(query);

    // Per spec, if respondsTo is true we must not abstain.
    // If the transformation is null we must deny, since we cannot abstain.
    return modifiedQuery == null ? QueryResponse.DENY : filter.query(modifiedQuery);
  }

  /** Returns a modified {@link Query} */
  @Nullable
  protected abstract R transformQuery(Q query);

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.filter.getRelevantEvents();
  }

  @Override
  public Stream<Filter> dependencies() {
    return Stream.of(this.filter);
  }
}
