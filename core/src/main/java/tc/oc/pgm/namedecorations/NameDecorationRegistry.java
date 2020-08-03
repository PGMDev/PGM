package tc.oc.pgm.namedecorations;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.party.Party;

public interface NameDecorationRegistry extends Listener {

  /**
   * Get the fully decorated name for this player
   *
   * @param player The player to decorate
   * @param party The party this player is currently in
   * @return The name, decorated
   */
  String getDecoratedName(Player player, Party party);

  /**
   * Force-refresh this player's decoration
   *
   * @param uuid UUID of the player to refresh
   */
  void refreshPlayer(UUID uuid);

  /**
   * Set what name decoration provider this registry should use
   *
   * @param provider The name decoration provider to use
   */
  void setProvider(NameDecorationProvider provider);

  NameDecorationProvider getProvider();
}
