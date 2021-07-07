package tc.oc.pgm.integration;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.FriendIntegration;

public class FriendIntegrationImpl implements FriendIntegration {

  @Override
  public boolean isFriend(Player a, Player b) {
    return false;
  }
}
