package tc.oc.pgm.tnt;

import java.time.Duration;
import org.jetbrains.annotations.Nullable;

public record TNTProperties(
    @Nullable Float yield,
    @Nullable Float power,
    boolean instantIgnite,
    boolean blockDamage,
    @Nullable Duration fuse,
    int dispenserNukeLimit,
    float dispenserNukeMultiplier,
    boolean licensing,
    boolean friendlyDefuse) {}
