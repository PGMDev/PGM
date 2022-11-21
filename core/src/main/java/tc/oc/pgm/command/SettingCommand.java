package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.observers.ObserverToolsMatchModule;
import tc.oc.pgm.settings.SettingsMenu;
import tc.oc.pgm.util.text.TextFormatter;

// TODO: remove some of these when settings UI is released
public final class SettingCommand {

  public static final SettingCommand INSTANCE = new SettingCommand();

  public static SettingCommand getInstance() {
    return INSTANCE;
  }

  private SettingCommand() {}

  @CommandMethod("settings")
  @CommandDescription("Open the settings menu")
  public void settings(MatchPlayer player) {
    new SettingsMenu(player);
  }

  @CommandMethod("tools|observertools|ot")
  @CommandDescription("Open the observer tools menu")
  public void observerTools(MatchPlayer player, ObserverToolsMatchModule tools) {
    if (player.isObserving()) {
      tools.openMenu(player);
    } else {
      // TODO: reconsider when observer tools become settings
      throw exception("setting.observersOnly");
    }
  }

  @CommandMethod("setting <setting>")
  @CommandDescription("Get the value of a setting")
  public void setting(MatchPlayer player, @Argument("setting") SettingKey key) {
    final SettingValue value = player.getSettings().getValue(key);

    sendCurrentSetting(player, key, value);
    player.sendMessage(
        translatable(
            "setting.options",
            TextFormatter.list(
                Stream.of(key.getPossibleValues())
                    .map(option -> text(option.getName(), NamedTextColor.GRAY))
                    .collect(Collectors.toList()),
                NamedTextColor.WHITE)));
  }

  @CommandMethod("toggle|set <setting> [value]")
  @CommandDescription("Toggle or set the value of a setting")
  public void toggle(
      MatchPlayer player,
      @Argument("setting") SettingKey key,
      @Argument("value") SettingValue value) {
    final Settings setting = player.getSettings();
    final SettingValue old = setting.getValue(key);

    if (value == null) {
      setting.toggleValue(key);
      value = setting.getValue(key);
    } else if (old != value) {
      setting.setValue(key, value);
    }

    if (old == value) {
      sendCurrentSetting(player, key, old);
    } else {
      player.sendMessage(
          translatable(
              "setting.set",
              text(key.getName()),
              text(old.getName(), NamedTextColor.GRAY),
              text(value.getName(), NamedTextColor.GREEN)));
      key.update(player);
    }
  }

  private void sendCurrentSetting(MatchPlayer player, SettingKey key, SettingValue value) {
    player.sendMessage(
        translatable(
            "setting.get", text(key.getName()), text(value.getName(), NamedTextColor.GREEN)));
  }
}
