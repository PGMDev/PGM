package tc.oc.pgm.commands;


import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.map.PGMMap;


@CommandContainer
public class CycleCommands {

  @Command(
          name = "cycle",
          desc = "Queues a cycle to the next map in a certain amount of seconds",
          descFooter = "[seconds] [mapname]")
  public static void cycle(
      CommandSender sender,
      Match match,
      MatchManager matchManager,
      Duration countdown,
      @Switch(name ='f', desc = "force") boolean force,
      @Arg(desc = "The map that end-user wants to cycle to", def = "next") @Text PGMMap map)
  {
    CycleMatchModule cmm = match.needMatchModule(CycleMatchModule.class);

    if (match.isRunning() && !force && !cmm.getConfig().runningMatch()) {
      throw new IllegalStateException(
          AllTranslations.get().translate("command.admin.cycle.matchRunning", sender));
    }

    if (map != null && matchManager.getMapOrder().getNextMap() != map) {
      matchManager.getMapOrder().setNextMap(map);
    }
    cmm.startCountdown(countdown);
  }

  @Command(
          name = "recycle",
          aliases = {"rematch"},
          desc = "Reload (cycle to) the current map",
          descFooter = "[seconds]")
  public static void recycle(
      CommandSender sender,
      Match match,
      MatchManager matchManager,
      Duration duration,
      @Switch(name ='f', desc = "force") boolean force)
  {
    cycle(sender, match, matchManager, duration, force, match.getMap());
  }
}
