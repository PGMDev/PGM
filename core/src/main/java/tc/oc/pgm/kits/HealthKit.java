package tc.oc.pgm.kits;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class HealthKit extends AbstractKit {
  protected final int halfHearts;

  public HealthKit(int halfHearts) {
    assertTrue(
        0 < halfHearts && halfHearts <= 20,
        "halfHearts must be greater than 0 and less than or equal to 20");
    this.halfHearts = halfHearts;
  }

  /** The force flag allows the kit to decrease the player's health */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    // Trying to set health > max throws an exception
    double newHealth = Math.min(halfHearts, player.getBukkit().getMaxHealth());
    if (force || player.getBukkit().getHealth() < newHealth) {
      player.getBukkit().setHealth(newHealth);
    }
  }
}
