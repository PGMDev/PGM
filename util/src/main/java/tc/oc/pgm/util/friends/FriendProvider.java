package tc.oc.pgm.util.friends;

import java.util.UUID;
import org.bukkit.entity.Player;

/** Interface which returns whether two {@link Player}s are friends * */
public interface FriendProvider {

  String METADATA_KEY = "friend-provider";

  FriendProvider DEFAULT = new NoopFriendProvider();

  boolean areFriends(UUID playerId, UUID otherId);

  public class NoopFriendProvider implements FriendProvider {
    @Override
    public boolean areFriends(UUID playerId, UUID otherId) {
      return false;
    }
  }
}
