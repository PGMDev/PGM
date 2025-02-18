package tc.oc.pgm.util.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.Players;

record PlayerRelationship(boolean reveal, boolean self, boolean friend, boolean squad) {
  public PlayerRelationship(@Nullable Player pl, @NotNull CommandSender viewer) {
    this(
        pl != null && Players.shouldReveal(viewer, pl),
        pl == viewer,
        pl != null && Players.isFriend(viewer, pl),
        pl != null && viewer instanceof Player && Integration.areInSquad((Player) viewer, pl));
  }
}
