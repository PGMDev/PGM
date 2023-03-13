package tc.oc.pgm.api.filter.query;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

public interface MatchQuery extends Query {
  Match getMatch();

  /** Extract the most specific {@link Filterable} possible from this query */
  @Nullable
  default Filterable<?> extractFilterable() {
    if (this instanceof PlayerQuery) return ((PlayerQuery) this).getPlayer();
    if (this instanceof PartyQuery) return ((PartyQuery) this).getParty();
    return this.getMatch();
  }

  default <T extends MatchModule> T moduleRequire(Class<T> cls) {
    return getMatch().needModule(cls);
  }

  default <T extends MatchModule> Optional<T> moduleOptional(Class<T> cls) {
    return Optional.ofNullable(getMatch().getModule(cls));
  }

  default <T extends ReactorFactory.Reactor> T reactor(ReactorFactory<T> factory) {
    return getMatch().needModule(FilterMatchModule.class).getReactor(factory);
  }

  @Nullable
  default <F extends Filterable<?>> F filterable(Class<F> type) {
    Filterable<?> filterable = extractFilterable();
    return filterable != null ? filterable.getFilterableAncestor(type) : null;
  }
}
