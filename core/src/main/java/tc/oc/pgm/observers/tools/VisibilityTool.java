package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class VisibilityTool implements InventoryMenuItem {

  @Override
  public Component getDisplayName() {
    return translatable("setting.visibility", NamedTextColor.YELLOW);
  }

  @Override
  public List<String> getLore(Player player) {
    Component status =
        translatable(
            isVisible(player) ? "setting.visibility.shown" : "setting.visibility.hidden",
            isVisible(player) ? NamedTextColor.GREEN : NamedTextColor.RED);
    Component lore = translatable("setting.visibility.lore", NamedTextColor.GRAY, status);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return isVisible(player) ? Material.EYE_OF_ENDER : Material.ENDER_PEARL;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    toggleObserverVisibility(player);
    menu.refreshWindow(player);
  }

  public boolean isVisible(Player player) {
    return PGM.get()
            .getMatchManager()
            .getPlayer(player)
            .getSettings()
            .getValue(SettingKey.OBSERVERS)
        == SettingValue.OBSERVERS_ON;
  }

  public void toggleObserverVisibility(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    Settings setting = matchPlayer.getSettings();
    setting.toggleValue(SettingKey.OBSERVERS);
    SettingKey.OBSERVERS.update(matchPlayer);
  }
}
