package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;

/** FriendIntegration - Determine whether two players are friends * */
public interface FriendIntegration {

  /**
   * Get whether the two players are friends
   *
   * @param a - The first player
   * @param b - The second player
   * @return true if friends, false if not
   */
  boolean isFriend(Player a, Player b);
}
