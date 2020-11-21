package tc.oc.pgm.friends;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.util.friends.FriendProvider;

/**
 * The FriendRegistry is responsible for managing a {@link FriendProvider} to consistently supply
 * whether two players are friends
 */
public interface FriendRegistry extends Listener, FriendProvider {

  default boolean areFriends(Player viewer, Player other) {
    return getProvider().areFriends(viewer, other);
  }

  FriendProvider getProvider();

  void setProvider(FriendProvider provider);
}
