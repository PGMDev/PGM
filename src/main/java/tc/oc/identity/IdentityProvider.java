package tc.oc.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

/** A factory for {@link Identity}s. Does nothing interesting for now. */
public interface IdentityProvider {
  Identity getIdentity(Player player);

  Identity getIdentity(UUID playerId, String username, @Nullable String nickname);
}
