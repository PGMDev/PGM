package tc.oc.pgm.util.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Players;

class PlayerRelationship {
  public final boolean reveal;
  public final boolean self;
  public final boolean friend;

  public PlayerRelationship(@Nullable Player pl, @NotNull CommandSender viewer) {
    this.reveal = pl != null && Players.shouldReveal(viewer, pl);
    this.self = pl == viewer;
    this.friend = pl != null && Players.isFriend(viewer, pl);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerRelationship)) return false;

    PlayerRelationship that = (PlayerRelationship) o;

    if (reveal != that.reveal) return false;
    if (self != that.self) return false;
    return friend == that.friend;
  }

  @Override
  public int hashCode() {
    int result = (reveal ? 1 : 0);
    result = 31 * result + (self ? 1 : 0);
    result = 31 * result + (friend ? 1 : 0);
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
        + '}';
  }
}
