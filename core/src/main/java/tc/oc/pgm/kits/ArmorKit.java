package tc.oc.pgm.kits;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.player.MatchPlayer;

public class ArmorKit extends AbstractKit {
  public static class ArmorItem {
    public final ItemStack stack;
    public final boolean locked;
    public final boolean teamColor;

    public ArmorItem(ItemStack stack, boolean locked, boolean teamColor) {
      this.stack = stack;
      this.locked = locked;
      this.teamColor = teamColor;
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

        if (entry.getValue().teamColor) {
          LeatherArmorMeta meta = (LeatherArmorMeta) wearing[slot].getItemMeta();
          meta.setColor(player.getParty().getFullColor());
          wearing[slot].setItemMeta(meta);
        }

        KitMatchModule kitMatchModule = player.getMatch().getModule(KitMatchModule.class);
        if (kitMatchModule != null) {
          kitMatchModule.lockArmorSlot(player, entry.getKey(), entry.getValue().locked);
        }
      }
    }
    player.getBukkit().getInventory().setArmorContents(wearing);
  }
}
