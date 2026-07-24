package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

public record StuckBehavior(
    Duration after,
    @Nullable Action<? super Match> stuckAction,
    @Nullable RelocationType moveTo,
    @Nullable Integer moveToIndex,
    @Nullable RelocationMethod method,
    boolean giveUp, boolean despawn) {
}
