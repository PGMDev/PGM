package tc.oc.pgm.menu;

import fr.minuskube.inv.ClickableItem;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.text.TextTranslations;

/** @see InventoryMenu * */
public interface MenuItem {

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
   * Action that should be performed once item is clicked
   *
   * @param player The player who clicked
   * @param type The type of click
   */
  void onClick(Player player, ClickType type);

  /**
   * Gets the action to perform when this item is clicked in an inventory menu.
   *
   * <p>Note: only override if you need access to the {@link InventoryClickEvent} otherwise use
   * {@link MenuItem#onClick(Player, ClickType)}
   *
   * @return a consumer for the inventory click event
   */
  default Consumer<InventoryClickEvent> getAction() {
    return context -> {
      Player player = (Player) context.getWhoClicked();
      onClick(player, context.getClick());
      context.setCurrentItem(createItem(player));
    };
  }

  /**
   * Called by {@link #createItem(Player)} after standard changes has been done. When possible this
   * method should be overridden instead of {@link #createItem(Player)}.
   */
  default ItemMeta modifyMeta(ItemMeta meta) {
    return meta;
  }

  /**
   * Creates a {@link ClickableItem} for the given player Item & action are linked together here
   *
   * @param player The player to display this item to
   * @return a clickable item used in inventory menus
   */
  default ClickableItem getClickableItem(Player player) {
    return ClickableItem.of(createItem(player), getAction());
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
