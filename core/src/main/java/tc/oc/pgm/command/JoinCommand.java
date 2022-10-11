package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.command.graph.Sender;
import tc.oc.pgm.join.JoinMatchModule;

public final class JoinCommand {

  @Command(
      aliases = {"join", "play"},
      desc = "Join the match",
      usage = "[team] - defaults to random",
      flags = "f",
      perms = Permissions.JOIN)
  public void join(Sender.Player sender, @Switch('f') boolean force, @Nullable Party team) {
    if (team != null && !(team instanceof Competitor)) {
      leave(sender);
      return;
    }

    final JoinMatchModule join = sender.getMatch().needModule(JoinMatchModule.class);
    if (force && sender.hasPermission(Permissions.JOIN_FORCE)) {
      join.forceJoin(sender.getPlayer(), (Competitor) team);
    } else {
      join.join(sender.getPlayer(), (Competitor) team);
    }
  }

  @Command(
      aliases = {"leave", "obs", "spectator", "spec"},
      desc = "Leave the match",
      perms = Permissions.LEAVE)
  public void leave(Sender.Player sender) {
    sender.getMatch().needModule(JoinMatchModule.class).leave(sender.getPlayer());
  }
}
