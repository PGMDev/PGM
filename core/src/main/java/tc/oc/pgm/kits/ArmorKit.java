package tc.oc.pgm.kits;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.util.inventory.ArmorType;

public class ArmorKit extends AbstractKit {
  public static class ArmorItem {
    public final ItemStack stack;
    public final boolean locked;

    public ArmorItem(ItemStack stack, boolean locked) {
      this.stack = stack;
      this.locked = locked;
    }
  }

  private final Map<ArmorType, ArmorItem> armor;

  public ArmorKit(Map<ArmorType, ArmorItem> armor) {
    this.armor = armor;
  }

  public Map<ArmorType, ArmorItem> getArmor() {
    return armor;
  }

  /**
   * If force is true, existing armor is always replaced. If false, it is never replaced. TODO:
   * repair armor, upgrade world
   */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    ItemStack[] wearing = player.getBukkit().getInventory().getArmorContents();
    for (Map.Entry<ArmorType, ArmorItem> entry : this.armor.entrySet()) {
      int slot = entry.getKey().ordinal();
      if (force || wearing[slot] == null || wearing[slot].getType() == Material.AIR) {
        wearing[slot] = entry.getValue().stack.clone();
        ItemModifier.apply(wearing[slot], player);
      }
    }
    player.getBukkit().getInventory().setArmorContents(wearing);
  }
}
