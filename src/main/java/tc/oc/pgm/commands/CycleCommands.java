package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.rotation.MapOrder;

public class CycleCommands {

  @Command(
      aliases = {"cycle"},
      desc = "Queues a cycle to the next map in a certain amount of seconds",
      usage = "[seconds] [mapname]",
      flags = "f",
      perms = Permissions.START)
  public static void cycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      Duration countdown,
      @Switch('f') boolean force,
      @Default("next") @Text MapInfo map)
      throws CommandException {
    CycleMatchModule cmm = match.needMatchModule(CycleMatchModule.class);

    if (match.isRunning() && !force && !cmm.getConfig().runningMatch()) {
      throw new CommandException(
          AllTranslations.get().translate("command.admin.cycle.matchRunning", sender));
    }

    if (map != null && mapOrder.getNextMap() != map) {
      mapOrder.setNextMap(map);
    }
    cmm.startCountdown(countdown);
  }

  @Command(
      aliases = {"recycle", "rematch"},
      desc = "Reload (cycle to) the current map",
      usage = "[seconds]",
      flags = "f",
      perms = Permissions.START)
  public static void recycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      Duration duration,
      @Switch('f') boolean force)
      throws CommandException {
    cycle(sender, match, mapOrder, duration, force, match.getMap());
  }
}
