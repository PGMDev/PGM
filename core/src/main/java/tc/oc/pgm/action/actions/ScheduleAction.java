package tc.oc.pgm.action.actions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

public class ScheduleAction<B extends Filterable<?>> extends AbstractAction<B> {
  protected final Duration duration;
  protected final Action<? super B> child;

  public ScheduleAction(Class<B> scope, Duration duration, Action<? super B> child) {
    super(scope);
    this.duration = duration;
    this.child = child;
  }

  @Override
  public void trigger(B t) {
    schedule(t, () -> child.trigger(t));
  }

  protected void schedule(B t, Runnable r) {
    t.getMatch()
        .getExecutor(MatchScope.RUNNING)
        .schedule(r, duration.toMillis(), TimeUnit.MILLISECONDS);
  }

  /** Specialization for running with a player, requires that the player did not change teams. */
  public static class Player extends ScheduleAction<MatchPlayer> {
    public Player(Duration duration, Action<? super MatchPlayer> child) {
      super(MatchPlayer.class, duration, child);
    }

    @Override
    public void trigger(MatchPlayer mp) {
      var party = mp.getParty();
      schedule(mp, () -> {
        if (mp.getParty() == party) child.trigger(mp);
      });
    }
  }
}
