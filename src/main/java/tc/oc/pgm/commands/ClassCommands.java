package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Text;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.util.StringUtils;

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
              + AllTranslations.get()
                  .translate("command.class.view.currentClass", player.getBukkit())
              + " "
              + ChatColor.GOLD
              + ChatColor.UNDERLINE
              + cls.getName());
      sender.sendMessage(
          ChatColor.DARK_PURPLE
              + AllTranslations.get()
                  .translate("command.class.view.list", player.getBukkit())
                  .replace("'/classes'", ChatColor.GOLD + "'/classes'" + ChatColor.DARK_PURPLE));
    } else {
      PlayerClass result = StringUtils.bestFuzzyMatch(search, classModule.getClasses(), 0.9);

      if (result == null) {
        throw new CommandException(
            AllTranslations.get()
                .translate("command.class.select.classNotFound", player.getBukkit()));
      }

      if (!cls.canUse(player.getBukkit())) {
        throw new CommandException(
            AllTranslations.get()
                .translate(
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
            AllTranslations.get().translate("command.class.stickyClass", player.getBukkit()));
      }

      sender.sendMessage(
          ChatColor.GREEN
              + AllTranslations.get()
                  .translate(
                      "command.class.select.confirm",
                      player.getBukkit(),
                      ChatColor.GOLD.toString()
                          + ChatColor.UNDERLINE
                          + result.getName()
                          + ChatColor.GREEN));
      if (player.isParticipating()) {
        sender.sendMessage(
            ChatColor.GREEN
                + AllTranslations.get()
                    .translate("command.class.select.nextSpawn", player.getBukkit()));
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
        StringUtils.dashedChatMessage(
            ChatColor.GOLD + AllTranslations.get().translate("command.class.list.title", sender),
            "-",
            ChatColor.RED.toString()));
    int i = 1;
    boolean doesntHave = false;
    for (PlayerClass cls : classModule.getClasses()) {
      StringBuilder result = new StringBuilder();

      result.append(i++).append(". ");

      if (cls == senderClass) {
        result.append(ChatColor.GOLD);
      } else if (cls.canUse(bukkit)) {
        result.append(ChatColor.GREEN);
      } else {
        result.append(ChatColor.RED);
        doesntHave = true;
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

    if (doesntHave) {
      sender.sendMessage(
          StringUtils.dashedChatMessage(
              ChatColor.GOLD
                  + AllTranslations.get()
                      .translate("command.class.list.shop", sender)
                      .replace("oc.tc/shop", ChatColor.GREEN + "oc.tc/shop" + ChatColor.GOLD),
              "-",
              ChatColor.RED.toString()));
    }
  }

  private static ClassMatchModule getClassModule(Match match, CommandSender sender)
      throws CommandException {
    ClassMatchModule classModule = match.getModule(ClassMatchModule.class);
    if (classModule != null) {
      return classModule;
    } else {
      throw new CommandException(
          AllTranslations.get().translate("command.class.notEnabled", sender));
    }
  }
}
