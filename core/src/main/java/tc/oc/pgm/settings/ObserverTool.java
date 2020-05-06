package tc.oc.pgm.settings;

import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.menu.items.InventoryClickAction;

public interface ObserverTool extends InventoryClickAction {
  public Component getName();

  public ChatColor getColor();

  public List<String> getLore(Player player);

  public Material getMaterial(Player player);

  default ItemStack createItem(Player player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        getColor()
            + ChatColor.BOLD.toString()
            + ComponentRenderers.toLegacyText(getName(), player));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);
    return stack;
  }
}
