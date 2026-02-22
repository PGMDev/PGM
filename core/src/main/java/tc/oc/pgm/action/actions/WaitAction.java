package tc.oc.pgm.action.actions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

public class WaitAction<B extends Filterable<?>> extends AbstractAction<B> {
  private final Duration duration;
  private final Action<? super B> action;

  public WaitAction(Class<B> scope, Duration duration, Action<? super B> action) {
    super(scope);
    this.duration = duration;
    this.action = action;
  }

  @Override
  public void trigger(B t) {
    final Party playerTeam = (t instanceof MatchPlayer) ? ((MatchPlayer) t).getParty() : null;
    t.getMatch()
        .getExecutor(MatchScope.RUNNING)
        .schedule(
            () -> {
              if (!(t instanceof MatchPlayer) || ((MatchPlayer) t).getParty() == playerTeam) {
                action.trigger(t);
              }
            },
            duration.toMillis(),
            TimeUnit.MILLISECONDS);
  }
}
