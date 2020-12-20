package tc.oc.pgm.util.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.text.TextTranslations;

public interface InventoryMenuItem {

  Component getName();

  ChatColor getColor();

  List<String> getLore(Player player);

  Material getMaterial(Player player);

  void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType);

  default ItemStack createItem(Player player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        getColor()
            + ChatColor.BOLD.toString()
            + TextTranslations.translateLegacy(getName(), player));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);

    return stack;
  }
}
