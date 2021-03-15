package tc.oc.pgm.integration;

import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/** FriendIntegration - Determine whether two players are friends */
public interface FriendIntegration extends Integration {

  public static final String FRIEND_KEY = "pgm-friend";

  default void setFriend(Player player, Set<UUID> playerIds) {
    player.setMetadata(FRIEND_KEY, new FixedMetadataValue(getOwner(), playerIds));
  }

  static boolean isFriend(Player a, Player b) {
    if (a.hasMetadata(FRIEND_KEY)) {
      Set<UUID> playerIds = (Set<UUID>) a.getMetadata(FRIEND_KEY).get(0).value();
      return playerIds.contains(b.getUniqueId());
    }
    return false;
  }
}
