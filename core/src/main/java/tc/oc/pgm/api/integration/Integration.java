package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new FriendIntegration.FriendIntegrationImpl());

  public static void setFriendIntegration(FriendIntegration integration) {
    FRIENDS.set(integration);
  }

  public static void setFriends(Player a, Collection<UUID> playerIds) {
    FRIENDS.get().setFriends(a, playerIds);
  }

  public static boolean isFriend(Player a, Player b) {
    return FRIENDS.get().isFriend(a, b);
  }
}
