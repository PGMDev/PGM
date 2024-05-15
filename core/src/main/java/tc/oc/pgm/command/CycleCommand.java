package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public final class CycleCommand {

  @Command("cycle [duration] [map]")
  @CommandDescription("Cycle to the next match")
  @Permission(Permissions.START)
  public void cycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      @Argument("duration") Duration duration,
      @Argument("map") @FlagYielding MapInfo map,
      @Flag(value = "force", aliases = "f") boolean force) {
    if (match.isRunning() && !force) {
      throw exception("admin.matchRunning.cycle");
    }

    if (map != null && mapOrder.getNextMap() != map) {
      mapOrder.setNextMap(map);
      MapOrderCommand.sendSetNextMessage(map, sender, match);
    }

    match.needModule(CycleMatchModule.class).startCountdown(duration);
  }

  @Command("recycle|rematch [duration]")
  @CommandDescription("Reload (cycle to) the current map")
  @Permission(Permissions.START)
  public void recycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      @Argument("duration") Duration duration,
      @Flag(value = "force", aliases = "f") boolean force) {
    cycle(sender, match, mapOrder, duration, match.getMap(), force);
  }
}
