package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.material.Materials;

/**
 * Server-side approximation of whether a player is supported by terrain or special blocks/liquids.
 */
@NullMarked
public final class PlayerSupportState {
  public final boolean isGrounded;
  public final boolean isSwimming;
  public final boolean isClimbing;
  public final boolean isInLava;

  private PlayerSupportState(
      boolean isGrounded, boolean isSwimming, boolean isClimbing, boolean isInLava) {
    this.isGrounded = isGrounded;
    this.isSwimming = isSwimming;
    this.isClimbing = isClimbing;
    this.isInLava = isInLava;
  }

  public static PlayerSupportState of(Player player) {
    return of(player, player.getLocation());
  }

  public static PlayerSupportState of(Player player, Location location) {
    boolean isSwimming = Materials.isWater(location);
    boolean isClimbing = Materials.isClimbable(location);
    boolean isInLava = Materials.isLava(location);
    boolean isGrounded = PLAYER_UTILS.isGrounded(player, location);

    return new PlayerSupportState(isGrounded, isSwimming, isClimbing, isInLava);
  }

  public boolean isSupported() {
    return this.isGrounded || this.isSwimming || this.isClimbing;
  }
}
