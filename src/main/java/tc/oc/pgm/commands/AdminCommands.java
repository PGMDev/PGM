package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Range;
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
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.RestartListener;
import tc.oc.util.StringUtils;
import tc.oc.util.TimeUtils;

public class AdminCommands {

  @Command(
      aliases = {"timedrestart"},
      desc = "Queues a server restart after a certain amount of time",
      usage = "[seconds] - defaults to 30 seconds",
      flags = "f",
      perms = Permissions.STOP)
  public static void restart(
      CommandSender sender,
      Match match,
      @Default("30s") Duration duration,
      @Switch('f') boolean force)
      throws CommandException {
    // Countdown defers automatic restart, so don't allow excessively long times
    Duration countdown = TimeUtils.min(duration, Duration.standardMinutes(5));

    if (match.isRunning() && !force) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.restart.matchRunning", sender));
    }

    match.finish();
    RestartListener.get().queueRestart(match, countdown, "/restart command");
  }

  @Command(
      aliases = {"postponerestart", "pr"},
      usage = "[matches]",
      desc =
          "Cancels any queued restarts and postpones automatic restart to at least "
              + "the given number of matches from now (default and maximum is 10).",
      perms = Permissions.STOP)
  public static void postponeRestart(
      CommandSender sender, @Range(min = 0, max = 10) int matchNumber) {
    Integer matches = RestartListener.get().restartAfterMatches(matchNumber);

    if (matches == null) {
      RestartManager.get().cancelRestart();
      sender.sendMessage(ChatColor.RED + "Automatic match count restart disabled");
    } else if (matches > 0) {
      RestartManager.get().cancelRestart();
      sender.sendMessage(
          ChatColor.RED + "Server will restart automatically in " + matches + " matches");
    } else if (matches == 0) {
      sender.sendMessage(
          ChatColor.RED + "Server will restart automatically after the current match");
    }
  }

  @Command(
      aliases = {"queuerestart", "qr"},
      desc = "Restart the server at the next safe opportunity",
      perms = Permissions.STOP)
  public void queueRestart(CommandSender sender) {
    if (!RestartManager.get().isRestartRequested()) {
      RestartManager.get().requestRestart("/queuerestart commands");
    }
    sender.sendMessage(ChatColor.RED + "Server will restart automatically after the current match");
  }

  @Command(
      aliases = {"cancelrestart", "cr"},
      desc = "Cancels a previously requested restart",
      perms = Permissions.STOP)
  public void cancelRestart(CommandSender sender) {
    if (!RestartManager.get().isRestartRequested()) {
      RestartManager.get().cancelRestart();
    }
    sender.sendMessage(ChatColor.RED + "Server restart is now cancelled");
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
      CommandSender sender, @Switch('f') boolean force, @Text PGMMap map, MatchManager matchManager)
      throws CommandException {
    MatchManager mm = PGM.get().getMatchManager();
    boolean restartQueued = RestartManager.get().isRestartRequested();

    if (restartQueued && !force) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.setNext.restartQueued", sender));
    }

    matchManager.getMapOrder().setNextMap(map);

    if (restartQueued) {
      RestartManager.get().cancelRestart();
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
                    ChatColor.GOLD + map.getInfo().name + ChatColor.DARK_PURPLE));
  }

  @Command(
      aliases = {"cancel"},
      desc = "Cancels all active PGM countdowns and disables auto-start for the current match",
      perms = Permissions.STOP)
  public static void cancel(CommandSender sender, Match match) {
    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      TimeLimitMatchModule tlmm = match.getMatchModule(TimeLimitMatchModule.class);
      tlmm.cancel();
      tlmm.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needMatchModule(StartMatchModule.class).setAutoStart(false);
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
