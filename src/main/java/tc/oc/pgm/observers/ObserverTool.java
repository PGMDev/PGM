package tc.oc.pgm.observers;

import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.component.Component;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.gui.InventoryGUI;

public interface ObserverTool {

  public Component getName();

  public ChatColor getColor();

  public List<String> getLore(MatchPlayer player);

  public Material getMaterial(MatchPlayer player);

  public void onInventoryClick(InventoryGUI menu, MatchPlayer player);

  default ItemStack createItem(MatchPlayer player) {
    ItemStack stack = new ItemStack(getMaterial(player));
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        getColor()
            + ChatColor.BOLD.toString()
            + ComponentRenderers.toLegacyText(getName(), player.getBukkit()));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);

    return stack;
  }
}
