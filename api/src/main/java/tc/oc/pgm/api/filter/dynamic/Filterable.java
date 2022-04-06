package tc.oc.pgm.api.filter.dynamic;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.Query;

/**
 * An object that {@link Filter}s can be applied to.
 *
 * <p>Filterables serve as {@link Query}s about themselves in their current state, and <b>each
 * Filterable subtype should also extend the query type given for its {@link Q} parameter.</b>
 * Unlike {@link Query}s in general, which may be ephemeral value types, Filterables have a
 * persistent identity.
 *
 * <p>Filterables are composed in a hiearchy that mirrors the type relationships of their {@link Q}
 * parameters. If query type B extends query type A then every Filterable<B> is contained in some
 * Filterable<A>. Aside from this, the types of the Filterables may not have any particular
 * relationship.
 */
public interface Filterable<Q extends MatchQuery> extends MatchQuery {

  /** The (single) Filterable that contains this one, or empty if this is a top-level object. */
  @Nullable
  Filterable<? super Q> getFilterableParent();

  /**
   * Return the enclosing Filterable of the given subtype, if any.
   *
   * <p>This object is returned if it extends the given type.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  default <F extends Filterable<?>> F getFilterableAncestor(Class<F> type) {
    if (type.isInstance(this)) {
      return (F) this;
    } else {
      @Nullable Filterable<? super Q> parent = getFilterableParent();
      return parent == null ? null : (F) parent;
    }
  }

  /**
   * Return all {@link Filterable} objects that this object is directly composed of.
   *
   * <p>This object is NOT included in the result, nor are indirect components i.e. grandchildren,
   * etc.
   */
  Collection<? extends Filterable<? extends Q>> getFilterableChildren();

  /**
   * Return all individual objects of the given Filterable subtype that this object is composed of,
   * directly or indirectly, possibly including this object itself.
   *
   * <p>This method simply tests this object's type, and recurses on all {@link
   * #getFilterableChildren()}. Subclasses should provide a more efficient implementation, if
   * possible.
   */
  @SuppressWarnings("unchecked")
  default <R extends Filterable<?>> Collection<? extends R> getFilterableDescendants(
      Class<R> type) {
    final Collection<R> result =
        getFilterableChildren().stream()
            .flatMap(child -> child.getFilterableDescendants(type).stream())
            .collect(Collectors.toList());
    if (type.isInstance(this)) {
      result.add((R) this);
    }
    return result;
  }
}
