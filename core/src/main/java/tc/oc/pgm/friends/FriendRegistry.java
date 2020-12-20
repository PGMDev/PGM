package tc.oc.pgm.friends;

import java.util.UUID;
import org.bukkit.event.Listener;
import tc.oc.pgm.util.friends.FriendProvider;

/**
 * The FriendRegistry is responsible for managing a {@link FriendProvider} to consistently supply
 * whether two players are friends
 */
public interface FriendRegistry extends Listener, FriendProvider {

  default boolean areFriends(UUID viewerId, UUID otherId) {
    return getProvider().areFriends(viewerId, otherId);
  }

  FriendProvider getProvider();

  void setProvider(FriendProvider provider);
}
