package tc.oc.pgm.observers;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class ObserverToolsInventoryMenuItem implements InventoryMenuItem {

  private final InventoryMenu observerToolsMenu;

  public ObserverToolsInventoryMenuItem(InventoryMenu observerToolsMenu) {
    this.observerToolsMenu = observerToolsMenu;
  }

  @Override
  public Component getName() {
    return translatable("setting.displayName");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.AQUA;
  }

  @Override
  public List<String> getLore(Player player) {
    return Lists.newArrayList(
        TextTranslations.translateLegacy(
            translatable("setting.lore", NamedTextColor.GRAY), player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.DIAMOND;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    observerToolsMenu.display(player);
  }
}
