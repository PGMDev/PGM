package tc.oc.util.bukkit.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;

public class Identities {
  private Identities() {}

  public static Identity current(Player player) {
    return PGM.get().getIdentityProvider().getIdentity(player);
  }

  public static Identity from(UUID playerId, String username, @Nullable String nickname) {
    return PGM.get().getIdentityProvider().getIdentity(playerId, username, nickname);
  }
}
