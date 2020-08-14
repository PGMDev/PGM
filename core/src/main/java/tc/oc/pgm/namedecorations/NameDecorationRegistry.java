package tc.oc.pgm.namedecorations;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
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
   * Get the fully decorated name as a Component
   *
   * <p>Note: Allows for prefix/suffix hover events
   *
   * @param player The player to decorate
   * @param party The party this player is currently in
   * @return The name, decorated, in component form
   */
  default Component getDecoratedNameComponent(Player player, Party party) {
    return TextComponent.of(getDecoratedName(player, party));
  }

  /**
   * Set what name decoration provider this registry should use
   *
   * @param provider The name decoration provider to use
   */
  void setProvider(NameDecorationProvider provider);

  NameDecorationProvider getProvider();
}
