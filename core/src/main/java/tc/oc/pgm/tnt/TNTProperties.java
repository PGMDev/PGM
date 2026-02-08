package tc.oc.pgm.tnt;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
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
