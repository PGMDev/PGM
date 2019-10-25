package tc.oc.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.PGMUtil;

public class Identities {
  private Identities() {}

  public static Identity current(Player player) {
    return PGMUtil.get().getIdentityProvider().getIdentity(player);
  }

  public static Identity from(UUID playerId, String username, @Nullable String nickname) {
    return PGMUtil.get().getIdentityProvider().getIdentity(playerId, username, nickname);
  }
}
