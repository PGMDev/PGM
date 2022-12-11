package tc.oc.pgm.integrations;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.FriendIntegration;

public class NoopFriendIntegration implements FriendIntegration {

  @Override
  public boolean isFriend(Player a, Player b) {
    return false;
  }
}
