package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;

/** FriendIntegration - Determine whether two players are friends */
public interface FriendIntegration {

  public static final String FRIEND_KEY = "pgm-friend";

  void setFriends(Player player, Collection<UUID> playerIds);

  boolean isFriend(Player a, Player b);

  public class FriendIntegrationImpl implements FriendIntegration {

    @Override
    public void setFriends(Player player, Collection<UUID> playerIds) {
      player.setMetadata(FRIEND_KEY, new FixedMetadataValue(PGM.get(), playerIds));
    }

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
