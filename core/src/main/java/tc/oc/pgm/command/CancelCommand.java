package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public final class CancelCommand {

  @Command(
      aliases = {"cancel", "cancelrestart", "cr"},
      desc = "Cancels all countdowns",
      perms = Permissions.STOP)
  public void cancel(Audience audience, Match match) {
    if (RestartManager.isQueued()) {
      match.callEvent(new CancelRestartEvent());
      audience.sendMessage(translatable("admin.cancelRestart.restartUnqueued", NamedTextColor.RED));
      return;
    }

    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      final TimeLimitMatchModule limits = match.getModule(TimeLimitMatchModule.class);
      limits.cancel();
      limits.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needModule(StartMatchModule.class).setAutoStart(false);
    audience.sendMessage(translatable("admin.cancelCountdowns", NamedTextColor.GREEN));
  }
}
