package tc.oc.pgm.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextTranslations;

public interface InventoryMenuItem {

  Component getName();

  ChatColor getColor();

  List<String> getLore(MatchPlayer player);

  Material getMaterial(MatchPlayer player);

  void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType);

  default ItemStack createItem(MatchPlayer player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        getColor()
            + ChatColor.BOLD.toString()
            + TextTranslations.translateLegacy(getName(), player.getBukkit()));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);

    return stack;
  }
}
