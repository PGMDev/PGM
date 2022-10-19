package tc.oc.pgm.kits.tag;

import com.google.common.collect.ImmutableSet;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.tag.ItemTag;

public class ItemModifier {

  public static final ItemTag<Boolean> TEAM_COLOR = ItemTag.newBoolean("team-color");

  public static final ImmutableSet<Material> COLOR_AFFECTED =
      ImmutableSet.of(
          Material.WOOL,
          Material.CARPET,
          Material.STAINED_CLAY,
          Material.STAINED_GLASS,
          Material.STAINED_GLASS_PANE);

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
      item.setDurability(getWoolColor(player.getParty().getDyeColor()));
    }
  }

  @SuppressWarnings("deprecation")
  private static byte getWoolColor(DyeColor color) {
    return color.getWoolData();
  }
}
