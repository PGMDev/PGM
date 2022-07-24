package tc.oc.pgm.filters.matcher.match;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterListener;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.matcher.player.ParticipatingFilter;
import tc.oc.pgm.filters.operator.AllFilter;

public class PlayerCountFilter extends TypedFilter.Impl<MatchQuery>
    implements ReactorFactory<PlayerCountFilter.Reactor> {
  private final Filter filter;
  private final Range<Integer> range;

  public PlayerCountFilter(
      Filter filter, Range<Integer> range, boolean participants, boolean observers) {
    if (!observers) filter = AllFilter.of(ParticipatingFilter.PARTICIPATING, filter);
    if (!participants) filter = AllFilter.of(ParticipatingFilter.OBSERVING, filter);
    this.filter = filter;
    this.range = range;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.copyOf(
        Iterables.concat(
            filter.getRelevantEvents(), ImmutableList.of(PlayerPartyChangeEvent.class)));
  }

  @Override
  public Reactor createReactor(Match match, FilterMatchModule fmm) {
    return new Reactor(match, fmm);
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return query.reactor(this).response();
  }

  protected final class Reactor extends ReactorFactory.Reactor
      implements FilterListener<MatchPlayer> {

    private final Set<MatchPlayer> players = new HashSet<>();

    Reactor(Match match, FilterMatchModule fmm) {
      super(match, fmm);
      fmm.onChange(MatchPlayer.class, filter, this);
    }

    boolean response() {
      return range.contains(players.size());
    }

    @Override
    public void filterQueryChanged(MatchPlayer filterable, boolean response) {
      final boolean before = response();

      if (response) players.add(filterable);
      else players.remove(filterable);

      if (before != response()) invalidate(filterable.getMatch());
    }
  }
}
