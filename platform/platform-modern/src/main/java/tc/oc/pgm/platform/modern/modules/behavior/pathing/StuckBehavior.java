package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.time.Duration;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

public class StuckBehavior {
  private final Duration after;
  private final @Nullable Action<? super Match> stuckAction;
  private final @Nullable RelocationType moveTo;
  private final @Nullable Integer moveToIndex;
  private final @Nullable RelocationMethod method;
  private final boolean giveUp;
  private final boolean despawn;

  public StuckBehavior(
      Duration after,
      @Nullable Action<? super Match> stuckAction,
      @Nullable RelocationType moveTo,
      @Nullable Integer moveToIndex,
      @Nullable RelocationMethod method,
      boolean giveUp,
      boolean despawn) {
    this.after = after;
    this.stuckAction = stuckAction;
    this.moveTo = moveTo;
    this.moveToIndex = moveToIndex;
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

  public @Nullable RelocationType getMoveTo() {
    return moveTo;
  }

  public @Nullable Integer getMoveToIndex() {
    return moveToIndex;
  }

  public @Nullable RelocationMethod getMethod() {
    return method;
  }

  public boolean isGiveUp() {
    return giveUp;
  }

  public boolean isDespawn() {
    return despawn;
  }
}
