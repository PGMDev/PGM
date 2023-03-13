package tc.oc.pgm.filters.matcher.player;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.TypedFilter;

/**
 * Base for filters that apply to *online*, participating players. The base class returns DENY if
 * the player is currently offline or observing, and abstains from non-player queries.
 *
 * <p>This should only be inherited by filters that absolutely require an online {@link MatchPlayer}
 * to match against. Generally, player filters should not rely on the player's current state, and
 * instead use only the properties of the {@link PlayerQuery} itself.
 */
public abstract class ParticipantFilter extends TypedFilter.Impl<PlayerQuery> {

  @Override
  public Class<? extends PlayerQuery> queryType() {
    return PlayerQuery.class;
  }

  protected abstract boolean matches(PlayerQuery query, MatchPlayer player);

  @Override
  public boolean matches(PlayerQuery query) {
    MatchPlayer player = query.getPlayer();
    return player != null && player.getParty() instanceof Competitor && matches(query, player);
  }
}
