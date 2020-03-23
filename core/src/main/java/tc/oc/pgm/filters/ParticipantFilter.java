package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Base for filters that apply to *online*, participating players. The base class returns DENY if
 * the player is currently offline or observing, and abstains from non-player queries.
 *
 * <p>This should only be inherited by filters that absolutely require an online {@link MatchPlayer}
 * to match against. Generally, player filters should not rely on the player's current state, and
 * instead use only the properties of the {@link PlayerQuery} itself.
 */
public abstract class ParticipantFilter extends TypedFilter<PlayerQuery> {

  @Override
  public Class<? extends PlayerQuery> getQueryType() {
    return PlayerQuery.class;
  }

  protected abstract QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player);

  @Override
  protected final QueryResponse queryTyped(PlayerQuery query) {
    MatchPlayer player = query.getMatch().getPlayer(query.getPlayerId());
    if (player != null && player.getParty() instanceof Competitor) {
      return queryPlayer(query, player);
    } else {
      return QueryResponse.DENY;
    }
  }
}
