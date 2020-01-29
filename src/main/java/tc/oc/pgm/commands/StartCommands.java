package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;

public class StartCommands {

  @Command(
      aliases = {"start", "begin"},
      desc = "Queues the start of the match in a certain amount of seconds",
      usage = "[countdown time] [huddle time]",
      perms = Permissions.START)
  public static void start(CommandSender sender, Match match, Duration countdown, Duration huddle)
      throws CommandException {
    StartMatchModule smm = match.needModule(StartMatchModule.class);

    if (match.isRunning()) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.start.matchRunning", sender));
    }
    if (match.isFinished()) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.start.matchFinished", sender));
    }

    if (smm.canStart(true)) {
      match.getCountdown().cancelAll(StartCountdown.class);
      smm.forceStartCountdown(countdown, huddle);
    } else {
      ComponentRenderers.send(
          sender,
          new PersonalizedText(
              new PersonalizedTranslatable("command.admin.start.unknownState"),
              net.md_5.bungee.api.ChatColor.RED));
      for (UnreadyReason reason : smm.getUnreadyReasons(true)) {
        ComponentRenderers.send(
            sender,
            new PersonalizedText(net.md_5.bungee.api.ChatColor.RED)
                .text(" * ")
                .extra(reason.getReason()));
      }
    }
  }

  @Command(
      aliases = {"autostart"},
      desc = "Enable or disable match auto-start",
      usage = "[on|off]",
      perms = Permissions.START)
  public static void autostart(CommandSender sender, Match match, @Nullable String toggle)
      throws CommandException {
    StartMatchModule smm = match.needModule(StartMatchModule.class);

    boolean autoStart;
    if (toggle != null) {
      switch (toggle) {
        case "on":
          autoStart = true;
          break;
        case "off":
          autoStart = false;
          break;
        default:
          throw new CommandException("Invalid usage. Use [on|off] as the arguments.");
      }
    } else {
      autoStart = !smm.isAutoStart();
    }

    smm.setAutoStart(autoStart);

    if (autoStart) {
      sender.sendMessage(
          ChatColor.GREEN
              + AllTranslations.get().translate("command.admin.autoStartEnabled", sender));
      smm.autoStartCountdown();
    } else {
      sender.sendMessage(
          ChatColor.BLUE
              + AllTranslations.get().translate("command.admin.autoStartDisabled", sender));
    }
  }
}
