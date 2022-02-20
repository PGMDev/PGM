package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Clears the player's inventory and status effects, including anything they may be holding on the
 * cursor
 */
public class ClearItemsKit extends AbstractKit {
  private final boolean items;
  private final boolean armor;
  private final boolean effects;

  public ClearItemsKit(boolean items, boolean armor, boolean effects) {
    this.items = items;
    this.armor = armor;
    this.effects = effects;
  }

  public boolean clearsItems() {
    return items;
  }

  public boolean clearsArmor() {
    return armor;
  }

  public boolean clearsEffects() {
    return effects;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    if (this.armor) {
      player.getBukkit().getInventory().setArmorContents(new ItemStack[4]);
    }
    if (this.effects) {
      for (PotionEffect potion : player.getBukkit().getActivePotionEffects()) {
        player.getBukkit().removePotionEffect(potion.getType());
      }
    }
    if (this.items) {
      player.getBukkit().getInventory().clear();
      player.getBukkit().getOpenInventory().setCursor(null);
    }
  }
}
