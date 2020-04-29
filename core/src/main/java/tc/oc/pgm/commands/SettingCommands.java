package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class SettingCommands {

  @Command(
      aliases = {"setting"},
      desc = "Get the value of a setting",
      usage = "[setting name]")
  public static void setting(CommandSender sender, MatchPlayer player, SettingKey key)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(TextTranslations.translate("command.onlyPlayers", sender));

    final SettingValue value = player.getSettings().getValue(key);

    player.sendMessage(
        new PersonalizedTranslatable(
            "setting.get",
            new PersonalizedText(key.getName()),
            new PersonalizedText(value.getName(), ChatColor.GREEN)));
    player.sendMessage(
        TranslatableComponent.of(
            "setting.options",
            TextFormatter.list(
                Stream.of(key.getPossibleValues())
                    .map(option -> TextComponent.of(option.getName(), TextColor.GRAY))
                    .collect(Collectors.toList()),
                TextColor.WHITE)));
  }

  @Command(
      aliases = {"toggle", "set"},
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
}
