package tc.oc.pgm.platform.modern.modules.behavior.evade;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;

public record EvadeBehavior(
    boolean panic,
    @Nullable Duration panicDuration,
    @Nullable Float avoidRange,
    @Nullable Filter avoidFilter) {}
