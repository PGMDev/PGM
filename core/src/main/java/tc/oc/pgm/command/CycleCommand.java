package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Maybe;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.time.Duration;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public final class CycleCommand {

  @Command(
      aliases = {"cycle"},
      desc = "Cycle to the next match",
      flags = "f",
      perms = Permissions.START)
  public void cycle(
      Match match,
      MapOrder mapOrder,
      @Maybe Duration duration,
      @Text @Default("next") MapInfo map,
      @Switch('f') boolean force) {
    if (match.isRunning() && !force) {
      throw exception("admin.matchRunning.cycle");
    }

    if (map != null && mapOrder.getNextMap() != map) {
      mapOrder.setNextMap(map);
    }

    match.needModule(CycleMatchModule.class).startCountdown(duration);
  }

  @Command(
      aliases = {"recycle", "rematch"},
      desc = "Reload (cycle to) the current map",
      usage = "[seconds]",
      flags = "f",
      perms = Permissions.START)
  public void recycle(
      Match match, MapOrder mapOrder, @Maybe Duration duration, @Switch('f') boolean force) {
    cycle(match, mapOrder, duration, match.getMap(), force);
  }
}
