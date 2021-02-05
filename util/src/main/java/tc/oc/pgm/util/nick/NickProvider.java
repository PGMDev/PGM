package tc.oc.pgm.util.nick;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface NickProvider {

  String METADATA_KEY = "nick-provider";

  NickProvider DEFAULT = new NoopNickProvider();

  Optional<String> getNick(UUID playerId);

  default String getPlayerName(Player player) {
    return getNick(player.getUniqueId()).isPresent()
        ? getNick(player.getUniqueId()).get()
        : player.getName();
  }

  public class NoopNickProvider implements NickProvider {
    @Override
    public Optional<String> getNick(UUID playerId) {
      return Optional.empty();
    }
  }
}
