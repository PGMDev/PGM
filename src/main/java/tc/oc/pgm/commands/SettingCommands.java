package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import com.google.common.collect.Lists;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.TranslationUtils;

@CommandContainer
public class SettingCommands {

  @Command(
      name = "setting",
      desc = "Get the value of a setting",
      descFooter = "[setting name]")
  public static void setting(CommandSender sender, MatchPlayer player, SettingKey key)
  {
    if (player == null)
      throw new IllegalStateException(AllTranslations.get().translate("command.onlyPlayers", sender));

    final SettingValue value = player.getSettings().getValue(key);

    player.sendMessage(
        new PersonalizedTranslatable(
            "command.setting.get",
            new PersonalizedText(key.getName()),
            new PersonalizedText(value.getName(), ChatColor.GREEN)));
    player.sendMessage(
        new PersonalizedTranslatable(
            "command.setting.options",
            TranslationUtils.legacyList(
                sender,
                (input) -> ChatColor.WHITE + input,
                (input) -> ChatColor.GRAY + input,
                Lists.newArrayList(key.getPossibleValues()))));
  }

  @Command(
      name = "toggle",
      aliases = {"set"},
      desc = "Toggle or set the value of a setting",
      descFooter = "[setting name] <option>")
  public static void toggle(
      CommandSender sender, MatchPlayer player, SettingKey key, @Nullable String query)
  {
    if (player == null)
      throw new IllegalStateException(AllTranslations.get().translate("command.onlyPlayers", sender));

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
              "command.setting.get",
              new PersonalizedText(key.getName()),
              new PersonalizedText(old.getName(), ChatColor.GREEN)));
    } else {
      player.sendMessage(
          new PersonalizedTranslatable(
              "command.setting.set",
              new PersonalizedText(key.getName()),
              new PersonalizedText(old.getName(), ChatColor.GRAY),
              new PersonalizedText(value.getName(), ChatColor.GREEN)));
      key.update(player);
    }
  }
}
