package tc.oc.pgm.util.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.Players;

class PlayerRelationship {
  public final boolean reveal;
  public final boolean self;
  public final boolean friend;
  public final boolean squad;

  public PlayerRelationship(@Nullable Player pl, @NotNull CommandSender viewer) {
    this.reveal = pl != null && Players.shouldReveal(viewer, pl);
    this.self = pl == viewer;
    this.friend = pl != null && Players.isFriend(viewer, pl);
    this.squad =
        pl != null && viewer instanceof Player && Integration.areInSquad((Player) viewer, pl);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PlayerRelationship that = (PlayerRelationship) o;

    if (reveal != that.reveal) return false;
    if (self != that.self) return false;
    if (friend != that.friend) return false;
    return squad == that.squad;
  }

  @Override
  public int hashCode() {
    int result = (reveal ? 1 : 0);
    result = 31 * result + (self ? 1 : 0);
    result = 31 * result + (friend ? 1 : 0);
    result = 31 * result + (squad ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PlayerRelationship{"
        + "reveal="
        + reveal
        + ", self="
        + self
        + ", friend="
        + friend
        + ", squad="
        + squad
        + '}';
  }
}
