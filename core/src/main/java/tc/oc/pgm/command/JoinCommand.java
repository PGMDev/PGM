package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;

public final class JoinCommand {

  @Command(
      aliases = {"join", "play"},
      desc = "Join the match",
      usage = "[team] - defaults to random",
      flags = "f",
      perms = Permissions.JOIN)
  public void join(
      Match match, MatchPlayer player, @Switch('f') boolean force, @Nullable Party team) {
    if (team != null && !(team instanceof Competitor)) {
      leave(player, match);
      return;
    }

    final JoinMatchModule join = match.needModule(JoinMatchModule.class);
    if (force && player.getBukkit().hasPermission(Permissions.JOIN_FORCE)) {
      join.forceJoin(player, (Competitor) team);
    } else {
      join.join(player, (Competitor) team);
    }
  }

  @Command(
      aliases = {"leave", "obs", "spectator", "spec"},
      desc = "Leave the match",
      perms = Permissions.LEAVE)
  public void leave(MatchPlayer player, Match match) {
    match.needModule(JoinMatchModule.class).leave(player);
  }
}
