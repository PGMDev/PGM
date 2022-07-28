package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;

public class KillStreakFilter extends ParticipantFilter {
  private final Range<Integer> range;
  private final boolean repeat;

  public KillStreakFilter(Range<Integer> range, boolean repeat) {
    this.range = range;
    this.repeat = repeat;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(MatchPlayerDeathEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    int streak = query.moduleRequire(KillRewardMatchModule.class).getKillStreak(player.getId());
    if (this.repeat && streak > 0) {
      int modulo =
          this.range.upperEndpoint() - (this.range.upperBoundType() == BoundType.CLOSED ? 0 : 1);
      streak = 1 + (streak - 1) % modulo;
    }
    return this.range.contains(streak);
  }
}
