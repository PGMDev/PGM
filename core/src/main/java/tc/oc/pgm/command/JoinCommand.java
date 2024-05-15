package tc.oc.pgm.command;

import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;

public final class JoinCommand {

  @Command("join|play [team]")
  @CommandDescription("Join the match")
  @Permission(Permissions.JOIN)
  public void join(
      JoinMatchModule joiner,
      MatchPlayer player,
      @Flag(value = "force", aliases = "f") boolean force,
      @Argument("team") @FlagYielding Party team) {
    if (team != null && !(team instanceof Competitor)) {
      leave(joiner, player);
      return;
    }

    if (force && player.getBukkit().hasPermission(Permissions.JOIN_FORCE)) {
      joiner.forceJoin(player, (Competitor) team);
    } else {
      joiner.join(player, (Competitor) team);
    }
  }

  @Command("leave|obs|spectator|spec")
  @CommandDescription("Leave the match")
  @Permission(Permissions.LEAVE)
  public void leave(JoinMatchModule joiner, MatchPlayer player) {
    joiner.leave(player, JoinRequest.empty());
  }
}
