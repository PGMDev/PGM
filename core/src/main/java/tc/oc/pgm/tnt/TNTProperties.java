package tc.oc.pgm.tnt;

import java.time.Duration;
import org.jetbrains.annotations.Nullable;

public class TNTProperties {
  public final @Nullable Float yield;
  public final @Nullable Float power;
  public final boolean instantIgnite;
  public final boolean blockDamage;
  public final @Nullable Duration fuse;
  public final int dispenserNukeLimit;
  public final float dispenserNukeMultiplier;
  public final boolean licensing;
  public final boolean friendlyDefuse;

  public TNTProperties(
      @Nullable Float yield,
      @Nullable Float power,
      boolean instantIgnite,
      boolean blockDamage,
      @Nullable Duration fuse,
      int dispenserNukeLimit,
      float dispenserNukeMultiplier,
      boolean licensing,
      boolean friendlyDefuse) {
    this.yield = yield;
    this.power = power;
    this.instantIgnite = instantIgnite;
    this.blockDamage = blockDamage;
    this.fuse = fuse;
    this.dispenserNukeLimit = dispenserNukeLimit;
    this.dispenserNukeMultiplier = dispenserNukeMultiplier;
    this.licensing = licensing;
    this.friendlyDefuse = friendlyDefuse;
  }
}
