package tc.oc.pgm.platform.modern.modules.behavior.home;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.region.Region;

public record HomeBehavior(
    Region region,
    boolean wander,
    @Nullable Float strayDis,
    @Nullable Duration returnAfter) {
}
