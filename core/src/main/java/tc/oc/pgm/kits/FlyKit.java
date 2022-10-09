package tc.oc.pgm.kits;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

public class FlyKit extends AbstractKit {
  public static final float MIN = 0, MAX = 10;
  public static final float BASE_INTERNAL_SPEED = 0.1f;

  protected final boolean allowFlight;
  protected final @Nullable Boolean flying;
  protected final float flySpeedMultiplier;

  public FlyKit(boolean allowFlight, @Nullable Boolean flying, float flySpeedMultiplier) {
    assertTrue(
        flying == null || !(flying && !allowFlight),
        "Flying cannot be true if allow-flight is false");

    this.allowFlight = allowFlight;
    this.flying = flying;
    this.flySpeedMultiplier = flySpeedMultiplier;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    player.getBukkit().setAllowFlight(this.allowFlight);
    if (this.flying != null) {
      player.getBukkit().setFlying(this.flying);
    }

    player.getBukkit().setFlySpeed(BASE_INTERNAL_SPEED * flySpeedMultiplier);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    if (allowFlight) {
      player.getBukkit().setAllowFlight(false);
    }
    if (flying != null) {
      player.getBukkit().setFlying(!flying);
    }
    if (flySpeedMultiplier != 1) {
      player.getBukkit().setFlySpeed(BASE_INTERNAL_SPEED);
    }
  }
}
