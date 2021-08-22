package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;

public class LivesFilter extends ParticipantFilter {
  private final Range<Integer> range;

  public LivesFilter(Range<Integer> range) {
    this.range = range;
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    int lives = player.getMatch().needModule(BlitzMatchModule.class).getNumOfLives(player.getId());
    return QueryResponse.fromBoolean(this.range.contains(lives));
  }
}
