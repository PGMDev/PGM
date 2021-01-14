package tc.oc.pgm.util.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.text.TextTranslations;

/** @see InventoryMenu */
public interface InventoryMenuItem {

  /**
   * Gets the display name of this item, can be localized, colored, and decorated (will
   * automatically be bolded)
   */
  Component getDisplayName();

  /** Gets the lore for this item, each string is one line of text */
  List<String> getLore(Player player);

  /** Gets the {@link Material} of this item */
  Material getMaterial(Player player);

  /**
   * If this method is in a {@link InventoryMenu} this method will be called automatically by the
   * menu
   */
  void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType);

  /**
   * Called by {@link #createItem(Player)} after standard changes has been done. When possible this
   * method should be overridden instead of {@link #createItem(Player)}.
   */
  default ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  default ItemStack createItem(Player player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        TextTranslations.translateLegacy(
            getDisplayName().decoration(TextDecoration.BOLD, true), player));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(modifyMeta(meta));

    return stack;
  }
}
