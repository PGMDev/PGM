package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.settings.SettingsMenu;
import tc.oc.pgm.util.text.TextTranslations;

public class SettingsTool implements MenuItem {

  @Override
  public Component getDisplayName() {
    return translatable("settings.menu.title", NamedTextColor.AQUA, TextDecoration.BOLD);
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.ANVIL;
  }

  @Override
  public List<String> getLore(Player player) {
    Component lore = translatable("settings.menu.lore", NamedTextColor.GRAY);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public void onClick(Player player, ClickType type) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    new SettingsMenu(matchPlayer);
  }
}
