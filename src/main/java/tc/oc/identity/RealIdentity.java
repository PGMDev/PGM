package tc.oc.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Represents the {@link Identity} of a player who is not nicked. */
public class RealIdentity extends IdentityBase {

  public RealIdentity(UUID playerId, String username) {
    super(playerId, username);
  }

  @Override
  public @Nullable String getNickname() {
    return null;
  }

  @Override
  public boolean isRevealed(CommandSender viewer) {
    return true;
  }

  @Override
  public boolean isOnline(CommandSender viewer) {
    Player player = getPlayer();
    return player != null;
  }

  @Override
  public boolean isCurrent() {
    Player player = getPlayer();
    return player != null;
  }
}
