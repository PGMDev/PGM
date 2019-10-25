package tc.oc.pgm.filters;

import tc.oc.pgm.features.FeatureReference;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Party;

public class CarryingFlagFilter extends TypedFilter<IPartyQuery> {

  private final FeatureReference<? extends FlagDefinition> flag;

  public CarryingFlagFilter(FeatureReference<? extends FlagDefinition> flag) {
    this.flag = flag;
  }

  @Override
  public Class<? extends IPartyQuery> getQueryType() {
    return IPartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IPartyQuery query) {
    final Match match = query.getMatch();
    final Flag goal = this.flag.get().getGoal(match);
    if (goal == null) return QueryResponse.ABSTAIN;

    if (query instanceof IPlayerQuery) {
      MatchPlayer player = match.getPlayer(((IPlayerQuery) query).getPlayerId());
      return QueryResponse.fromBoolean(player != null && goal.isCarrying(player));
    } else {
      final Party party = query.getParty();
      return QueryResponse.fromBoolean(
          party instanceof Competitor && goal.isCarrying((Competitor) party));
    }
  }
}
