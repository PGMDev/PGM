package tc.oc.pgm.kits.tag;

import static tc.oc.pgm.util.material.ColorUtils.COLOR_UTILS;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.tag.ItemTag;

public class ItemModifier {

  public static final ItemTag<Boolean> TEAM_COLOR = ItemTag.newBoolean("team-color");

  // Apply per-player customizations of items.
  // This may be expanded on the future but currently only handles coloring armor & blocks.
  // The method is explicitly mutate-only, if you don't want side effects pass a clone.
  public static void apply(ItemStack item, MatchPlayer player) {
    if (!TEAM_COLOR.has(item)) return;
    TEAM_COLOR.clear(item);

    ItemMeta meta = item.getItemMeta();

    if (meta instanceof LeatherArmorMeta leather) {
      leather.setColor(player.getParty().getFullColor());
      item.setItemMeta(meta);
    } else if (COLOR_UTILS.isColorAffected(item.getType())) {
      COLOR_UTILS.setColor(item, player.getParty().getDyeColor());
    }
  }
}
