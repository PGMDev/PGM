package tc.oc.util.bukkit.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public class Identities {
  private Identities() {}

  public static Identity current(Player player) {
    return new RealIdentity(player.getUniqueId(), player.getName());
  }

  public static Identity from(UUID playerId, String username, @Nullable String nickname) {
    return new RealIdentity(playerId, username);
  }
}
