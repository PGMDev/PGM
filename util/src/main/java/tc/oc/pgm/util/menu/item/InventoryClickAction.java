package tc.oc.pgm.util.menu.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.InventoryMenu;

public interface InventoryClickAction {

  void onClick(InventoryMenu menu, Player player, ClickType clickType);
}
