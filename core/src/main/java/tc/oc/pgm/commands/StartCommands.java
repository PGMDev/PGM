package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.text.TextTranslations;

public class StartCommands {

  @Command(
      aliases = {"start", "begin"},
      desc = "Queues the start of the match in a certain amount of seconds",
      usage = "[countdown time] [huddle time]",
      perms = Permissions.START)
  public static void start(
      CommandSender sender, Match match, Duration countdown, @Nullable Duration huddle)
      throws CommandException {
    StartMatchModule smm = match.needModule(StartMatchModule.class);

    if (match.isRunning()) {
      throw new CommandException(TextTranslations.translate("admin.start.matchRunning", sender));
    }
    if (match.isFinished()) {
      throw new CommandException(TextTranslations.translate("admin.start.matchFinished", sender));
    }

    if (smm.canStart(true)) {
      match.getCountdown().cancelAll(StartCountdown.class);
      smm.forceStartCountdown(countdown, huddle == null ? Duration.ZERO : huddle);
    } else {
      ComponentRenderers.send(
          sender,
          new PersonalizedText(
              new PersonalizedTranslatable("admin.start.unknownState"),
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
          ChatColor.GREEN + TextTranslations.translate("admin.autoStart.enabled", sender));
      smm.autoStartCountdown();
    } else {
      sender.sendMessage(
          ChatColor.RED + TextTranslations.translate("admin.autoStart.disabled", sender));
    }
  }
}
