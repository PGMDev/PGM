package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Text;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class ClassCommands {

  @Command(
      aliases = {"class", "selectclass", "c", "cl"},
      desc = "Selects or views the player class")
  public static void selectClass(
      CommandSender sender, Match match, MatchPlayer player, @Nullable @Text String search)
      throws CommandException {
    ClassMatchModule classModule = getClassModule(match, sender);

    PlayerClass cls = classModule.getSelectedClass(player.getId());

    if (search == null) {
      // show current class
      sender.sendMessage(
          ChatColor.GREEN
              + TextTranslations.translate("match.class.current", player.getBukkit())
              + " "
              + ChatColor.GOLD
              + ChatColor.UNDERLINE
              + cls.getName());
      sender.sendMessage(
          ChatColor.DARK_PURPLE
              + TextTranslations.translate("match.class.view", player.getBukkit())
                  .replace("'/classes'", ChatColor.GOLD + "'/classes'" + ChatColor.DARK_PURPLE));
    } else {
      PlayerClass result = StringUtils.bestFuzzyMatch(search, classModule.getClasses(), 0.9);

      if (result == null) {
        throw new CommandException(
            TextTranslations.translate("match.class.notFound", player.getBukkit()));
      }

      if (!cls.canUse(player.getBukkit())) {
        throw new CommandException(
            TextTranslations.translate(
                "command.class.restricted",
                player.getBukkit(),
                ChatColor.GOLD,
                result.getName(),
                ChatColor.RED));
      }

      try {
        classModule.setPlayerClass(player.getId(), result);
      } catch (IllegalStateException e) {
        throw new CommandException(
            TextTranslations.translate("match.class.sticky", player.getBukkit()));
      }

      sender.sendMessage(
          ChatColor.GREEN
              + TextTranslations.translate(
                  "match.class.ok",
                  player.getBukkit(),
                  ChatColor.GOLD.toString()
                      + ChatColor.UNDERLINE
                      + result.getName()
                      + ChatColor.GREEN));
      if (player.isParticipating()) {
        sender.sendMessage(
            ChatColor.GREEN + TextTranslations.translate("match.class.queue", player.getBukkit()));
      }
    }
  }

  @Command(
      aliases = {"classlist", "classes", "listclasses", "cls"},
      desc = "Lists the classes available on this map")
  public static void listclasses(CommandSender sender, Match match) throws CommandException {
    Player bukkit = (Player) sender;
    ClassMatchModule classModule = getClassModule(match, sender);

    final PlayerClass senderClass = classModule.getSelectedClass(bukkit.getUniqueId());

    sender.sendMessage(
        ComponentUtils.dashedChatMessage(
            ChatColor.GOLD + TextTranslations.translate("match.class.title", sender),
            "-",
            ChatColor.RED.toString()));
    int i = 1;
    for (PlayerClass cls : classModule.getClasses()) {
      StringBuilder result = new StringBuilder();

      result.append(i++).append(". ");

      if (cls == senderClass) {
        result.append(ChatColor.GOLD);
      } else if (cls.canUse(bukkit)) {
        result.append(ChatColor.GREEN);
      } else {
        result.append(ChatColor.RED);
      }

      if (cls == senderClass) result.append(ChatColor.UNDERLINE);
      result.append(cls.getName());

      if (cls.getDescription() != null) {
        result
            .append(ChatColor.DARK_PURPLE)
            .append(" - ")
            .append(ChatColor.RESET)
            .append(cls.getDescription());
      }

      sender.sendMessage(result.toString());
    }
  }

  private static ClassMatchModule getClassModule(Match match, CommandSender sender)
      throws CommandException {
    ClassMatchModule classModule = match.getModule(ClassMatchModule.class);
    if (classModule != null) {
      return classModule;
    } else {
      throw new CommandException(TextTranslations.translate("match.class.notEnabled", sender));
    }
  }
}
