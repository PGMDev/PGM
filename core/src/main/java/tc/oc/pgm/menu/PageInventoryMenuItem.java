package tc.oc.pgm.menu;

import static net.kyori.adventure.text.Component.translatable;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;

public class PageInventoryMenuItem implements InventoryMenuItem {
  private final List<InventoryMenuItem> inventoryItems;
  private final int toPage;
  private final boolean next;
  private final Component inventoryTitle;

  PageInventoryMenuItem(
      Component inventoryTitle,
      List<InventoryMenuItem> inventoryItems,
      int currentPage,
      boolean next) {
    this.inventoryTitle = inventoryTitle;
    this.inventoryItems = inventoryItems;
    this.toPage = next ? ++currentPage : --currentPage;
    this.next = next;
  }

  @Override
  public Component getName() {
    return translatable(next ? "misc.nextPage" : "misc.previousPage");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.WHITE;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    return null;
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return Material.ARROW;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    InventoryMenuUtils.prettyMenu(player.getMatch(), inventoryTitle, inventoryItems, toPage)
        .display(player);
  }
}
