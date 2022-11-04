package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.FlagYielding;
import java.time.Duration;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public final class CycleCommand {

  @CommandMethod("cycle [duration] [map]")
  @CommandDescription("Cycle to the next match")
  @CommandPermission(Permissions.START)
  public void cycle(
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
    }

    match.needModule(CycleMatchModule.class).startCountdown(duration);
  }

  @CommandMethod("recycle|rematch [duration]")
  @CommandDescription("Reload (cycle to) the current map")
  @CommandPermission(Permissions.START)
  public void recycle(
      Match match,
      MapOrder mapOrder,
      @Argument("duration") Duration duration,
      @Flag(value = "force", aliases = "f") boolean force) {
    cycle(match, mapOrder, duration, match.getMap(), force);
  }
}
