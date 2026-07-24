package tc.oc.pgm.platform.modern.modules.behavior.looking;

import org.jspecify.annotations.Nullable;
import org.bukkit.util.Vector;

public record LookBehavior(
    boolean trackPlayer,
    @Nullable Vector trackPoint,
    RotationType rotation,
    float range,
    @Nullable Float restYaw,
    @Nullable Float restPitch) {
}
