package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.command.graph.Sender;
import tc.oc.pgm.cycle.CycleMatchModule;

import java.time.Duration;

import static tc.oc.pgm.util.text.TextException.exception;

public final class CycleCommand {

  @Command(
      aliases = {"cycle"},
      desc = "Cycle to the next match",
      flags = "f",
      perms = Permissions.START)
  public void cycle(
      MapOrder mapOrder,
      Sender sender,
      @Default("null") Duration duration,
      @Default("next") MapInfo map,
      @Switch('f') boolean force) {
    if (sender.getMatch().isRunning() && !force) {
      throw exception("admin.matchRunning.cycle");
    }

    if (map != null && mapOrder.getNextMap() != map) {
      mapOrder.setNextMap(map);
    }

    sender.getMatch().needModule(CycleMatchModule.class).startCountdown(duration);
  }

  @Command(
      aliases = {"recycle", "rematch"},
      desc = "Reload (cycle to) the current map",
      usage = "[seconds]",
      flags = "f",
      perms = Permissions.START)
  public void recycle(
      MapOrder mapOrder, Sender sender, @Default("null") Duration duration, @Switch('f') boolean force) {
    cycle(mapOrder, sender, duration, sender.getMatch().getMap(), force);
  }
}
