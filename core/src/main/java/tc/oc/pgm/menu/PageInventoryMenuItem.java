package tc.oc.pgm.menu;

import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;

public class PageInventoryMenuItem implements InventoryMenuItem {
  private final List<InventoryMenuItem> to;
  private final int toPage;
  private final boolean next;
  private final Component inventoryTitle;

  PageInventoryMenuItem(
      Component inventoryTitle, List<InventoryMenuItem> to, int currentPage, boolean next) {
    this.inventoryTitle = inventoryTitle;
    this.to = to;
    this.toPage = next ? ++currentPage : --currentPage;
    this.next = next;
  }

  @Override
  public Component getName() {
    return TranslatableComponent.of(next ? "misc.nextPage" : "misc.previousPage");
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
    InventoryMenuUtils.prettyMenu(player.getMatch(), inventoryTitle, to, toPage).display(player);
  }
}
