package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class VisibilityTool implements InventoryMenuItem {

  @Override
  public Component getName() {
    return translatable("setting.visibility");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.YELLOW;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component status =
        translatable(
            isVisible(player) ? "setting.visibility.shown" : "setting.visibility.hidden",
            isVisible(player) ? NamedTextColor.GREEN : NamedTextColor.RED);
    Component lore = translatable("setting.visibility.lore", NamedTextColor.GRAY, status);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return isVisible(player) ? Material.EYE_OF_ENDER : Material.ENDER_PEARL;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    toggleObserverVisibility(player);
    menu.refreshWindow(player);
  }

  public boolean isVisible(MatchPlayer player) {
    return player.getSettings().getValue(SettingKey.OBSERVERS) == SettingValue.OBSERVERS_ON;
  }

  public void toggleObserverVisibility(MatchPlayer player) {
    Settings setting = player.getSettings();
    setting.toggleValue(SettingKey.OBSERVERS);
    SettingKey.OBSERVERS.update(player);
  }
}
