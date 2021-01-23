package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class VerboseStatsInventoryMenuItem implements InventoryMenuItem {

  private final InventoryMenu verboseStatsMenu;

  VerboseStatsInventoryMenuItem(InventoryMenu verboseStatsMenu) {
    this.verboseStatsMenu = verboseStatsMenu;
  }

  @Override
  public Component getDisplayName() {
    return translatable("match.stats.title", NamedTextColor.GREEN, TextDecoration.BOLD);
  }

  @Override
  public List<String> getLore(Player player) {
    return Lists.newArrayList(
        TextTranslations.translateLegacy(
            translatable("setting.lore", NamedTextColor.GRAY), player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.PAPER;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    verboseStatsMenu.display(player);
  }
}
