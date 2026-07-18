package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

import java.time.Duration;

public class StuckBehavior {
  private final Duration after;
  private final @Nullable Action<? super Match> stuckAction;
  private final RelocationType moveTo;
  private final RelocationMethod method;
  private final boolean giveUp;
  private final boolean despawn;

  public StuckBehavior(
      Duration after,
      @Nullable Action<? super Match> stuckAction,
      RelocationType moveTo,
      RelocationMethod method,
      boolean giveUp,
      boolean despawn) {
    this.after = after;
    this.stuckAction = stuckAction;
    this.moveTo = moveTo;
    this.method = method;
    this.giveUp = giveUp;
    this.despawn = despawn;
  }

  public Duration getAfter() {
    return after;
  }

  public @Nullable Action<? super Match> getStuckAction() {
    return stuckAction;
  }

  public RelocationType getMoveTo() {
    return getMoveTo();
  }

  public RelocationMethod getMethod() {
    return method;
  }

  public boolean isGiveUp() {
    return giveUp;
  }

  public boolean isDespawn() {
    return despawn;
  }
}
