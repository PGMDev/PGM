package tc.oc.pgm.util.friends;

import org.bukkit.entity.Player;

/** Interface which returns whether two {@link Player}s are friends * */
public interface FriendProvider {

  String METADATA_KEY = "friend-provider";

  FriendProvider DEFAULT = new NoopFriendProvider();

  boolean areFriends(Player player, Player other);

  public class NoopFriendProvider implements FriendProvider {
    @Override
    public boolean areFriends(Player player, Player other) {
      return false;
    }
  }
}
