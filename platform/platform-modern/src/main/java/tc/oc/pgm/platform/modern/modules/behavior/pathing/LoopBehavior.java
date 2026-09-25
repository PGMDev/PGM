package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

public record LoopBehavior(
    boolean randomGoal,
    boolean reverse,
    @Nullable Integer times,
    @Nullable Action<? super Match> completionAction) {}
