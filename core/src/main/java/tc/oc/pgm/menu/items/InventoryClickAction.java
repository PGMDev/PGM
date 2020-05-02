package tc.oc.pgm.menu.items;

import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;

public interface InventoryClickAction {

  void onClick(InventoryMenu menu, MatchPlayer player, ClickType clickType);
}
