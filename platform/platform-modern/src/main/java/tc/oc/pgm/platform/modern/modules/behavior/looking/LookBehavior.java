package tc.oc.pgm.platform.modern.modules.behavior.looking;

import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public record LookBehavior(
    boolean trackPlayer,
    @Nullable Vector trackPoint,
    RotationType rotation,
    float range,
    @Nullable Float restYaw,
    @Nullable Float restPitch) {}
