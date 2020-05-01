package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import java.time.Duration;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.util.text.TextTranslations;

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
    CycleMatchModule cmm = match.needModule(CycleMatchModule.class);

    if (match.isRunning() && !force) {
      throw new CommandException(TextTranslations.translate("admin.matchRunning.cycle", sender));
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
