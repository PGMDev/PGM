package tc.oc.pgm.filters;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.IPlayerQuery;

/**
 * Base for filters that apply to *online*, participating players. The base class returns DENY if
 * the player is currently offline or observing, and abstains from non-player queries.
 *
 * <p>This should only be inherited by filters that absolutely require an online {@link MatchPlayer}
 * to match against. Generally, player filters should not rely on the player's current state, and
 * instead use only the properties of the {@link IPlayerQuery} itself.
 */
public abstract class ParticipantFilter extends TypedFilter<IPlayerQuery> {

  @Override
  public Class<? extends IPlayerQuery> getQueryType() {
    return IPlayerQuery.class;
  }

  protected abstract QueryResponse queryPlayer(IPlayerQuery query, MatchPlayer player);

  @Override
  protected final QueryResponse queryTyped(IPlayerQuery query) {
    MatchPlayer player = query.getMatch().getPlayer(query.getPlayerId());
    if (player != null && player.getParty() instanceof Competitor) {
      return queryPlayer(query, player);
    } else {
      return QueryResponse.DENY;
    }
  }
}
