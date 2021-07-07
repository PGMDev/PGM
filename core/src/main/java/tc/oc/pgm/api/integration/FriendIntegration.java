package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;

/** FriendIntegration - Determine whether two players are friends */
public interface FriendIntegration {

  boolean isFriend(Player a, Player b);
}
