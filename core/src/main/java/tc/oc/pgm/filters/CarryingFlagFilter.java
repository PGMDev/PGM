package tc.oc.pgm.filters;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;

public class CarryingFlagFilter extends TypedFilter<PartyQuery> {

  private final FeatureReference<? extends FlagDefinition> flag;

  public CarryingFlagFilter(FeatureReference<? extends FlagDefinition> flag) {
    this.flag = flag;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PartyQuery query) {
    final Match match = query.getMatch();
    final Flag goal = this.flag.get().getGoal(match);
    if (goal == null) return QueryResponse.ABSTAIN;

    if (query instanceof PlayerQuery) {
      MatchPlayer player = match.getPlayer(((PlayerQuery) query).getPlayerId());
      return QueryResponse.fromBoolean(player != null && goal.isCarrying(player));
    } else {
      final Party party = query.getParty();
      return QueryResponse.fromBoolean(
          party instanceof Competitor && goal.isCarrying((Competitor) party));
    }
  }
}
