package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.List;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public record PathingBehavior(
    @Nullable Vector start,
    @Nullable Float leash,
    List<PathingGoalBehavior> goals,
    @Nullable LoopBehavior loop,
    @Nullable StuckBehavior stuck) {}
