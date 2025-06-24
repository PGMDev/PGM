package tc.oc.pgm.stats.menu.items;

import static net.kyori.adventure.text.Component.translatable;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

/** The verbose stats menu item shown in the hotbar * */
public class VerboseStatsMenuItem implements MenuItem {

  @Override
  public Component getDisplayName() {
    return translatable("match.stats.title", NamedTextColor.GOLD, TextDecoration.BOLD);
  }

  @Override
  public List<String> getLore(Player player) {
    return List.of(TextTranslations.translateLegacy(
        translatable("setting.lore", NamedTextColor.GRAY), player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.PAPER;
  }

  @Override
  public void onClick(Player player, ClickType type) {}
}
