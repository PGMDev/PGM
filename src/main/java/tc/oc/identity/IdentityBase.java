package tc.oc.identity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.world.DeathOverride;

/** Functionality common to real and nicked identities */
public abstract class IdentityBase implements Identity {

  protected final UUID playerId;
  protected final String username;

  public IdentityBase(UUID playerId, String username) {
    this.playerId = checkNotNull(playerId);
    this.username = checkNotNull(username);
  }

  @Override
  public UUID getPlayerId() {
    return playerId;
  }

  @Override
  public Player getPlayer() {
    return Bukkit.getPlayer(getPlayerId());
  }

  @Override
  public String getRealName() {
    return username;
  }

  @Override
  public String getName(CommandSender viewer) {
    return isRevealed(viewer) ? getRealName() : getNickname();
  }

  @Override
  public boolean isDead(CommandSender viewer) {
    if (!isOnline(viewer)) return false;
    Player player = getPlayer();
    return player != null && DeathOverride.isDead(player);
  }

  @Override
  public boolean isFriend(CommandSender viewer) {
    return false;
  }

  @Override
  public boolean isSelf(UUID user) {
    return getPlayerId().equals(user);
  }

  @Override
  public boolean isSelf(Player player) {
    return getPlayerId().equals(player.getUniqueId());
  }

  @Override
  public boolean isSelf(CommandSender sender) {
    Player player = Bukkit.getPlayer(getPlayerId());
    return player != null && player.equals(sender);
  }

  @Override
  public boolean isCurrent(Player player) {
    return isSelf(player) && isCurrent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Identity)) return false;
    Identity identity = (Identity) o;
    return Objects.equals(getPlayerId(), identity.getPlayerId())
        && Objects.equals(getNickname(), identity.getNickname());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPlayerId(), getNickname());
  }
}
