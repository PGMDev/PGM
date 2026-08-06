package tc.oc.pgm.platform.modern.modules.behavior.combat;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;

public record CombatBehavior(
    HostilityType hostility,
    @Nullable Duration duration,
    float range,
    Duration interval,
    @Nullable Filter attackFilter) {}
