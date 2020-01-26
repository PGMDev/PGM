package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import tc.oc.component.Component;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;

public class VisibilityTool implements InventoryMenuItem {

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("observer.tools.visibility");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.YELLOW;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component status =
        new PersonalizedTranslatable(
                isVisible(player)
                    ? "observer.tools.visibility.shown"
                    : "observer.tools.visibility.hidden")
            .getPersonalizedText()
            .color(isVisible(player) ? ChatColor.GREEN : ChatColor.RED);
    Component lore =
        new PersonalizedTranslatable("observer.tools.visibility.lore", status)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return isVisible(player) ? Material.EYE_OF_ENDER : Material.ENDER_PEARL;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player) {
    toggleObserverVisibility(player);
    menu.refreshWindow(player);
  }

  public boolean isVisible(MatchPlayer player) {
    return player.getSettings().getValue(SettingKey.OBSERVERS) == SettingValue.OBSERVERS_ON;
  }

  public void toggleObserverVisibility(MatchPlayer player) {
    Settings setting = player.getSettings();
    setting.toggleValue(SettingKey.OBSERVERS);
  }
}
