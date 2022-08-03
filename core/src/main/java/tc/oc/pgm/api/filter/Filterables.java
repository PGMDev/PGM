package tc.oc.pgm.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

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

  static boolean isAssignable(
      Class<? extends Filterable<?>> child, Class<? extends Filterable<?>> parent) {
    for (Class<? extends Filterable<?>> scope : SCOPES) {
      if (scope == parent) return true;
      if (scope == child) return false;
    }
    return false;
  }

  static boolean isAssignable(Filterable<?> child, Class<? extends Filterable<?>> parent) {
    if (child == null) return false;
    if (parent.isInstance(child)) return true;
    return isAssignable(child.getFilterableParent(), parent);
  }

  // This fits better in XML utils, but that is in the util package which does not have access to
  // pgm classes
  @SuppressWarnings("unchecked")
  static <T extends Filterable<?>> Class<T> parse(Node node) throws InvalidXMLException {
    switch (node.getValueNormalize()) {
      case "player":
        return (Class<T>) MatchPlayer.class;
      case "team":
        return (Class<T>) Party.class;
      case "match":
        return (Class<T>) Match.class;
      default:
        throw new InvalidXMLException("Unknown scope, must be one of: player, team, match", node);
    }
  }
}
