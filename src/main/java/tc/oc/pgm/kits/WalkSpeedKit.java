package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class WalkSpeedKit extends AbstractKit {
  public static final float MIN = 0, MAX = 5;
  public static final float BUKKIT_DEFAULT = 0.2f;

  private final float speedMultiplier;

  public WalkSpeedKit(float speedMultiplier) {
    this.speedMultiplier = speedMultiplier;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    player.getBukkit().setWalkSpeed(BUKKIT_DEFAULT * this.speedMultiplier);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    player.getBukkit().setWalkSpeed(BUKKIT_DEFAULT);
  }
}
