package tc.oc.pgm.settings;

import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.menu.item.InventoryClickAction;

public interface ObserverTool extends InventoryClickAction {
  public Component getName();

  public TextColor getColor();

  public List<String> getLore(Player player);

  public Material getMaterial(Player player);

  default ItemStack createItem(Player player) {
    return new ItemBuilder()
        .material(getMaterial(player))
        .name(player, getName().color(getColor()).decoration(TextDecoration.BOLD, true))
        .lore(getLore(player).toArray(new String[] {}))
        .flags(ItemFlag.values())
        .build();
  }
}
