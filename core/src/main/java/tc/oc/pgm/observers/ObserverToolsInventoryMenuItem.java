package tc.oc.pgm.observers;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class ObserverToolsInventoryMenuItem implements InventoryMenuItem {

  private final InventoryMenu observerToolsMenu;

  public ObserverToolsInventoryMenuItem(InventoryMenu observerToolsMenu) {
    this.observerToolsMenu = observerToolsMenu;
  }

  @Override
  public Component getName() {
    return TranslatableComponent.of("setting.displayName");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.AQUA;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    return Lists.newArrayList(
        TextTranslations.translateLegacy(
            TranslatableComponent.of("setting.lore", TextColor.GRAY), player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return Material.DIAMOND;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    observerToolsMenu.display(player);
  }
}
