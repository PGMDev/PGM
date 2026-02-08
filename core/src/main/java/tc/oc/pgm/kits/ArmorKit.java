package tc.oc.pgm.kits;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.util.inventory.ArmorType;
import tc.oc.pgm.util.inventory.Slot;

public class ArmorKit extends AbstractKit {
  public record ArmorItem(ItemStack stack, boolean locked) {}

  private final Map<Slot.Armor, ArmorItem> armor;

  public ArmorKit(Map<Slot.Armor, ArmorItem> armor) {
    this.armor = armor;
  }

  @Deprecated(forRemoval = true)
  public Map<ArmorType, ArmorItem> getArmor() {
    var remapped = new HashMap<ArmorType, ArmorItem>();
    armor.forEach((slot, armorItem) -> remapped.put(slot.getArmorType(), armorItem));
    return remapped;
  }

  public Collection<ArmorItem> getArmorItems() {
    return armor.values();
  }

  /**
   * If force is true, existing armor is always replaced. If false, it is never replaced. TODO:
   * repair armor, upgrade world
   */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    this.armor.forEach((slot, item) -> {
      var wearing = slot.getItem(player);
      if (force || wearing == null) {
        slot.setItem(player, wearing = item.stack.clone());
        ItemModifier.apply(wearing, player);
      }
    });
  }
}
