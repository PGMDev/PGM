package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

/** Clears the player's inventory, including anything they may be holding on the cursor */
public class ClearItemsKit extends AbstractKit {
  private final boolean armor;

  public ClearItemsKit(boolean armor) {
    this.armor = armor;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    if (this.armor) {
      player.getBukkit().getInventory().setArmorContents(new ItemStack[4]);
    }
    player.getBukkit().getInventory().clear();
    player.getBukkit().getOpenInventory().setCursor(null);
  }
}
