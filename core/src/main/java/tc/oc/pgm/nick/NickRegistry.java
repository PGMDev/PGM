package tc.oc.pgm.nick;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.util.nick.NickProvider;

public interface NickRegistry extends Listener, NickProvider {

  default Optional<String> getNick(Player player) {
    return getNick(player.getUniqueId());
  }

  Optional<String> getNick(UUID playerId);

  void setProvider(@Nullable NickProvider provider);

  @Nonnull
  NickProvider getProvider();
}
