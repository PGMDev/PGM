package tc.oc.pgm.filters.matcher.player;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;

public class CarryingFlagFilter extends TypedFilter.Impl<PartyQuery> {

  private final FeatureReference<? extends FlagDefinition> flag;

  public CarryingFlagFilter(FeatureReference<? extends FlagDefinition> flag) {
    this.flag = flag;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(FlagStateChangeEvent.class);
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }

  @Override
  public boolean matches(PartyQuery query) {
    final Match match = query.getMatch();
    final Flag goal = this.flag.get().getGoal(match);
    if (goal == null) throw new IllegalStateException("Flag not found");

    if (query instanceof PlayerQuery) {
      MatchPlayer player = ((PlayerQuery) query).getPlayer();
      return player != null && goal.isCarrying(player);
    } else {
      final Party party = query.getParty();
      return party instanceof Competitor && goal.isCarrying((Competitor) party);
    }
  }
}
