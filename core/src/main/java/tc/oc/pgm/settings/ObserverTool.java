package tc.oc.pgm.settings;

import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.menu.item.InventoryClickAction;
import tc.oc.pgm.util.text.TextTranslations;

public interface ObserverTool extends InventoryClickAction {
  public Component getName();

  public TextColor getColor();

  public List<String> getLore(Player player);

  public Material getMaterial(Player player);

  default ItemStack createItem(Player player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        TextTranslations.translateLegacy(
            getName().color(getColor()).decoration(TextDecoration.BOLD, true), player));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);
    return stack;
  }
}
