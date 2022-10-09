package tc.oc.pgm.kits;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class MaxHealthKit extends AbstractKit {

  public static final double BUKKIT_DEFAULT = 20.0;

  private final double maxHealth;

  public MaxHealthKit(double maxHealth) {
    assertTrue(maxHealth > 0, "max health must be greater than zero");
    this.maxHealth = maxHealth;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    player.getBukkit().setMaxHealth(maxHealth);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    player.getBukkit().setMaxHealth(BUKKIT_DEFAULT);
  }
}
