package tc.oc.pgm.action.actions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

public class ScheduleAction<B extends Filterable<?>> extends AbstractAction<B> {
  protected final Duration after;
  protected final Action<? super B> child;

  public ScheduleAction(Class<B> scope, Duration after, Action<? super B> child) {
    super(scope);
    this.after = after;
    this.child = child;
  }

  // The query is stale once the schedule runs, but locals are shared with the rest of the frame
  @Override
  public void trigger(B t, ActionContext context) {
    var deferred = context.withoutQuery();
    schedule(t, () -> child.trigger(t, deferred));
  }

  protected void schedule(B t, Runnable r) {
    t.getMatch()
        .getExecutor(MatchScope.RUNNING)
        .schedule(r, after.toMillis(), TimeUnit.MILLISECONDS);
  }

  /** Specialization for running with a player, requires that the player did not change teams. */
  public static class Player extends ScheduleAction<MatchPlayer> {
    public Player(Duration after, Action<? super MatchPlayer> child) {
      super(MatchPlayer.class, after, child);
    }

    @Override
    public void trigger(MatchPlayer mp, ActionContext context) {
      var party = mp.getParty();
      var deferred = context.withoutQuery();
      schedule(mp, () -> {
        if (mp.getParty() == party) child.trigger(mp, deferred);
      });
    }
  }
}
