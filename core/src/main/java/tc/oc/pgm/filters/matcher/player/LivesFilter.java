package tc.oc.pgm.filters.matcher.player;

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
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return query
        .moduleOptional(BlitzMatchModule.class)
        .map(bmm -> range.contains(bmm.getNumOfLives(player.getId())))
        .orElse(false);
  }
}
