package tc.oc.pgm.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.FlagYielding;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;

public final class JoinCommand {

  @CommandMethod("join|play [team]")
  @CommandDescription("Join the match")
  @CommandPermission(Permissions.JOIN)
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

  @CommandMethod("leave|obs|spectator|spec")
  @CommandDescription("Leave the match")
  @CommandPermission(Permissions.LEAVE)
  public void leave(JoinMatchModule joiner, MatchPlayer player) {
    joiner.leave(player, JoinRequest.empty());
  }
}
