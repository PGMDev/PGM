package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.settings.SettingsMatchModule;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.text.TextTranslations;

public class SettingCommands {

  @Command(
      aliases = "toggle",
      desc = "Toggle or set the value of a setting",
      usage = "[setting name] <option>")
  public static void toggle(
      CommandSender sender, MatchPlayer player, SettingKey key, @Nullable String query)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(TextTranslations.translate("command.onlyPlayers", sender));

    final Settings setting = player.getSettings();
    final SettingValue old = setting.getValue(key);

    final SettingValue value;
    if (query == null) {
      setting.toggleValue(key);
      value = setting.getValue(key);
    } else {
      value = SettingValue.search(key, query);
      setting.setValue(key, value);
    }

    if (old == value) {
      player.sendMessage(
          new PersonalizedTranslatable(
              "setting.get",
              new PersonalizedText(key.getName()),
              new PersonalizedText(old.getName(), ChatColor.GREEN)));
    } else {
      player.sendMessage(
          new PersonalizedTranslatable(
              "setting.set",
              new PersonalizedText(key.getName()),
              new PersonalizedText(old.getName(), ChatColor.GRAY),
              new PersonalizedText(value.getName(), ChatColor.GREEN)));
      key.update(player);
    }
  }

  @Command(aliases = "settings", desc = "Open the settings GUI")
  public static void openSettings(CommandSender sender, MatchPlayer player)
      throws ArgumentException {
    if (player == null) {
      throw new ArgumentException(TextTranslations.translate("command.onlyPlayers", sender));
    }

    player.getMatch().getModule(SettingsMatchModule.class).openMenu(player);
  }
}
