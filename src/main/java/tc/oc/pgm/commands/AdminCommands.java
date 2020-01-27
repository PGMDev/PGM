package tc.oc.pgm.commands;



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
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.util.StringUtils;
import tc.oc.pgm.commands.annotations.Text;

import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Switch;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@CommandContainer
//TODO add permissions
public class AdminCommands {


  @Command(
          name = "queuerestart",
          aliases = {"qr"},
          desc = "Restart the server at the next opportunity",
          descFooter = "[seconds] - defaults to 30 seconds")
  public void queueRestart(
          CommandSender sender, Match match, int duration/*TODO Add a default duration*/, @Switch(name = 'f', desc = "force") boolean force)
  {
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
          name = "cancelrestart",
          aliases = {"cr"},
          desc = "Cancels a previously requested restart",
          descFooter = "")
  public void cancelRestart(CommandSender sender) {
    if (RestartManager.isQueued()) {
      PGM.get().getServer().getPluginManager().callEvent(new CancelRestartEvent());
      sender.sendMessage(ChatColor.RED + "Server restart is now cancelled");
    } else {
      sender.sendMessage(ChatColor.RED + "No restart is currently queued.");
    }
  }

  @Command(
          name = "end",
          aliases = {"finish"},
          desc = "Ends the current running match, optionally with a winner",
          descFooter = "[competitor]")
  public static void end(CommandSender sender, Match match, @Nullable @Text String target)
  {
    Competitor winner = StringUtils.bestFuzzyMatch(target, getCompetitorMap(sender, match), 0.9);

    if (target != null && winner == null)
      throw new NoSuchElementException(
              AllTranslations.get().translate("command.competitorNotFound", sender));

    boolean ended = match.finish(winner);

    if (!ended)
      throw new UnknownError(
              AllTranslations.get().translate("command.admin.end.unknownError", sender));
  }

  @Command(
          name= "setnext",
          aliases = {"sn"},
          desc = "Sets the next map.  Note that the rotation will go to this map then resume as normal.",
          descFooter = "[map name]")
  public static void setNext(
          CommandSender sender, @Switch( name= 'f', desc="force") boolean force, @Text PGMMap map, MatchManager matchManager)
  {
    MatchManager mm = PGM.get().getMatchManager();

    if (RestartManager.isQueued() && !force) {
      throw new IllegalStateException(
              AllTranslations.get().translate("command.admin.setNext.restartQueued", sender));
    }

    matchManager.getMapOrder().setNextMap(map);

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
                            ChatColor.GOLD + map.getInfo().name + ChatColor.DARK_PURPLE));
  }

  @Command(
          name = "cancel",
          aliases = {""},
          desc = "Cancels all active PGM countdowns and disables auto-start for the current match")
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
          name = "pgm",
          desc = "Reload the PGM configuration")
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


