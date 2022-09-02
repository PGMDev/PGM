package tc.oc.pgm.filters.operator;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.matcher.party.CompetitorFilter;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;

/**
 * Adapts a {@link CompetitorFilter}, which depends on the party in the query, into a filter with an
 * explicit {@link TeamFactory} that can respond to any {@link MatchQuery}.
 *
 * <p>The team can also be omitted, in which case this delegates to {@link
 * CompetitorFilter#matchesAny(MatchQuery)}.
 *
 * <p>Note that this is not a {@link SingleFilterFunction}, because it is (currently) entirely
 * transparent to the user. That is, it cannot be created directly through XML, it is only used to
 * implement other filters.
 *
 * <p>As a future enhancement, we could potentially allow it to be created directly, which might
 * look something like this:
 *
 * <p><team team="red-team"> <score>5</score> </team>
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TeamFilterAdapter extends TypedFilter.Impl<MatchQuery> {

  private final Optional<XMLFeatureReference<TeamFactory>> team;
  private final CompetitorFilter filter;

  public TeamFilterAdapter(
      Optional<XMLFeatureReference<TeamFactory>> team, CompetitorFilter filter) {
    this.team = team;
    this.filter = filter;
  }

  @Override
  public boolean isDynamic() {
    return filter.isDynamic();
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return filter.getRelevantEvents();
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return team.map(
            teamFactoryFeatureReference ->
                query
                    .moduleOptional(TeamMatchModule.class)
                    .map(
                        tmm ->
                            filter.matches(query, tmm.getTeam(teamFactoryFeatureReference.get())))
                    .orElse(false))
        .orElseGet(() -> filter.matchesAny(query));
  }
}
