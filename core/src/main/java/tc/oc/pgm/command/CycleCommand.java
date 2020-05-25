package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.util.text.TextException;

public final class CycleCommand {

  @Command(
      aliases = {"cycle"},
      desc = "Cycle to the next match",
      flags = "f",
      perms = Permissions.START)
  public void cycle(
      Match match, Config config, @Nullable Duration duration, @Switch('f') boolean force) {
    if (match.isRunning() && !force) {
      throw TextException.of("admin.matchRunning.cycle");
    }

    match
        .needModule(CycleMatchModule.class)
        .startCountdown(duration == null ? config.getCycleTime() : duration);
  }
}
