package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.time.Duration;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

public record PathingGoalBehavior(
    Vector destination,
    @Nullable Duration idle,
    @Nullable Action<? super Match> completionAction,
    @Nullable Float goalRadius) {
}
