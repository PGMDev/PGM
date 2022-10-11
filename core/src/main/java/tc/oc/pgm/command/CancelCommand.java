package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.command.graph.Sender;
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
  public void cancel(Sender context) {
    if (RestartManager.isQueued()) {
      context.getMatch().callEvent(new CancelRestartEvent());
      context.sendMessage(translatable("admin.cancelRestart.restartUnqueued", NamedTextColor.RED));
      return;
    }

    if (!context.getMatch().getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      final TimeLimitMatchModule limits = context.getMatch().getModule(TimeLimitMatchModule.class);
      limits.cancel();
      limits.setTimeLimit(null);
    }

    context.getMatch().getCountdown().cancelAll();
    context.getMatch().needModule(StartMatchModule.class).setAutoStart(false);
    context.sendMessage(translatable("admin.cancelCountdowns", NamedTextColor.GREEN));
  }
}
