package tc.oc.pgm.kits.tag;

import java.util.EnumSet;
import java.util.HashSet;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.nms.material.Colorable;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

public class ItemModifier {

  public static final ItemTag<Boolean> TEAM_COLOR = ItemTag.newBoolean("team-color");

  public static EnumSet<Material> COLOR_AFFECTED = getColorableMaterials();

  private static EnumSet<Material> getColorableMaterials() {
    HashSet<Material> colorableMaterials = new HashSet<>();
    for (Material value : Material.values()) {
      if (MaterialDataProvider.from(value) instanceof Colorable) {
        colorableMaterials.add(value);
      }
    }
    return EnumSet.copyOf(colorableMaterials);
  }

  // Apply per-player customizations of items.
  // This may be expanded on the future but currently only handles coloring armor & blocks.
  // The method is explicitly mutate-only, if you don't want side effects pass a clone.
  public static void apply(ItemStack item, MatchPlayer player) {
    if (!TEAM_COLOR.has(item)) return;
    TEAM_COLOR.clear(item);

    ItemMeta meta = item.getItemMeta();

    if (meta instanceof LeatherArmorMeta) {
      LeatherArmorMeta leather = (LeatherArmorMeta) meta;
      leather.setColor(player.getParty().getFullColor());
      item.setItemMeta(meta);
    } else if (COLOR_AFFECTED.contains(item.getType())) {
      MaterialData materialData = MaterialDataProvider.from(item);
      if (materialData instanceof Colorable) {
        ((Colorable) materialData).setColor(player.getParty().getDyeColor());
        materialData.apply(item);
      }
    }
  }
}
