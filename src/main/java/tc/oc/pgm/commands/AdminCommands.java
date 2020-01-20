package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapOrder;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.util.StringUtils;

public class AdminCommands {

  @Command(
      aliases = {"queuerestart", "qr"},
      desc = "Restart the server at the next safe opportunity",
      usage = "[seconds] - defaults to 30 seconds",
      flags = "f",
      perms = Permissions.STOP)
  public void queueRestart(
      CommandSender sender, Match match, @Default("30") int duration, @Switch('f') boolean force)
      throws CommandException {
    RestartManager.queueRestart(
        "Restart requested via /queuerestart command", Duration.standardSeconds(duration));

    sender.sendMessage(
        ChatColor.RED + "Server will restart automatically at the next safe opportunity.");

    if (force && match.isRunning()) {
      match.finish();
    } else {
      PGM.get().getServer().getPluginManager().callEvent(new RequestRestartEvent());
    }
  }

  @Command(
      aliases = {"cancelrestart", "cr"},
      desc = "Cancels a previously requested restart",
      perms = Permissions.STOP)
  public void cancelRestart(CommandSender sender) {
    if (RestartManager.isQueued()) {
      PGM.get().getServer().getPluginManager().callEvent(new CancelRestartEvent());
      sender.sendMessage(ChatColor.RED + "Server restart is now cancelled");
    } else {
      sender.sendMessage(ChatColor.RED + "No restart is currently queued.");
    }
  }

  @Command(
      aliases = {"end", "finish"},
      desc = "Ends the current running match, optionally with a winner",
      usage = "[competitor]",
      perms = Permissions.STOP)
  public static void end(CommandSender sender, Match match, @Nullable @Text String target)
      throws CommandException {
    Competitor winner = StringUtils.bestFuzzyMatch(target, getCompetitorMap(sender, match), 0.9);

    if (target != null && winner == null)
      throw new CommandException(
          AllTranslations.get().translate("command.competitorNotFound", sender));

    boolean ended = match.finish(winner);

    if (!ended)
      throw new CommandException(
          AllTranslations.get().translate("command.admin.end.unknownError", sender));
  }

  @Command(
      aliases = {"setnext", "sn"},
      desc =
          "Sets the next map.  Note that the rotation will go to this map then resume as normal.",
      usage = "[map name]",
      flags = "f",
      perms = Permissions.SETNEXT)
  public static void setNext(
      CommandSender sender, @Switch('f') boolean force, @Text MapInfo map, MapOrder mapOrder)
      throws CommandException {
    MatchManager mm = PGM.get().getMatchManager();

    if (RestartManager.isQueued() && !force) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.setNext.restartQueued", sender));
    }

    mapOrder.setNextMap(map);

    if (RestartManager.isQueued()) {
      RestartManager.cancelRestart();
      sender.sendMessage(
          ChatColor.GREEN
              + AllTranslations.get()
                  .translate("command.admin.cancelRestart.restartUnqueued", sender));
    }

    sender.sendMessage(
        ChatColor.DARK_PURPLE
            + AllTranslations.get()
                .translate(
                    "command.admin.set.success",
                    sender,
                    ChatColor.GOLD + map.getName() + ChatColor.DARK_PURPLE));
  }

  @Command(
      aliases = {"cancel"},
      desc = "Cancels all active PGM countdowns and disables auto-start for the current match",
      perms = Permissions.STOP)
  public static void cancel(CommandSender sender, Match match) {
    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      TimeLimitMatchModule tlmm = match.getModule(TimeLimitMatchModule.class);
      tlmm.cancel();
      tlmm.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needModule(StartMatchModule.class).setAutoStart(false);
    sender.sendMessage(
        ChatColor.GREEN + AllTranslations.get().translate("command.admin.cancel.success", sender));
  }

  @Command(
      aliases = {"pgm"},
      desc = "Reload the PGM configuration",
      perms = Permissions.RELOAD)
  public void pgm(CommandSender sender) {
    PGM.get().reloadConfig();
    sender.sendMessage(
        ChatColor.GREEN + AllTranslations.get().translate("command.admin.pgm", sender));
  }

  private static Map<String, Competitor> getCompetitorMap(CommandSender sender, Match match) {
    return match.getCompetitors().stream()
        .map(competitor -> new AbstractMap.SimpleEntry<>(competitor.getName(sender), competitor))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
