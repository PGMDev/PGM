package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class VerboseStatsInventoryMenuItem implements InventoryMenuItem {

  private final InventoryMenu verboseStatsMenu;

  VerboseStatsInventoryMenuItem(InventoryMenu verboseStatsMenu) {
    this.verboseStatsMenu = verboseStatsMenu;
  }

  @Override
  public Component getName() {
    return translatable("match.stats.title", NamedTextColor.GREEN, TextDecoration.BOLD);
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.GREEN;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    return Lists.newArrayList(
        TextTranslations.translateLegacy(
            translatable("setting.lore", NamedTextColor.GRAY), player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return Material.PAPER;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    verboseStatsMenu.display(player);
  }
}
