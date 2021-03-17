package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;

/** FriendIntegration - Determine whether two players are friends */
public interface FriendIntegration {

  boolean isFriend(Player a, Player b);

  public class FriendIntegrationImpl implements FriendIntegration {

    public static final String FRIEND_KEY = "pgm-friend";

    @Override
    public boolean isFriend(Player a, Player b) {
      if (a.hasMetadata(FRIEND_KEY)) {
        Collection<UUID> playerIds = (Collection<UUID>) a.getMetadata(FRIEND_KEY).get(0).value();
        return playerIds.contains(b.getUniqueId());
      }
      return false;
    }
  }
}
