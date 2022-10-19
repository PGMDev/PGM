package tc.oc.pgm.namedecorations;

import static net.kyori.adventure.text.Component.text;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.named.NameDecorationProvider;

/**
 * The NameDecorationRegistry will take care of using a Provider, and applying the changes to the
 * player's display name, as well as (implementation-dependant) provide a cache for prefixes &
 * suffixes.
 */
public interface NameDecorationRegistry extends Listener, NameDecorationProvider {

  /**
   * Get the fully decorated name for this player
   *
   * @param player The player to decorate
   * @param partyColor The color of the party this player is currently in
   * @return The name, decorated
   */
  String getDecoratedName(Player player, ChatColor partyColor);

  /**
   * Get the fully decorated name as a Component
   *
   * <p>Note: Allows for prefix/suffix hover events
   *
   * @param player The player to decorate
   * @param partyColor The color of the party this player is currently in
   * @return The name, decorated, in component form
   */
  default Component getDecoratedNameComponent(Player player, ChatColor partyColor) {
    return text(getDecoratedName(player, partyColor));
  }

  /**
   * Set the name decoration provider this registry should use, if null, a NO-OP provider will be
   * used
   *
   * @param provider The name decoration provider to use
   */
  void setProvider(@Nullable NameDecorationProvider provider);

  @NotNull
  NameDecorationProvider getProvider();

  default String getPrefix(UUID uuid) {
    return getProvider().getPrefix(uuid);
  }

  default String getSuffix(UUID uuid) {
    return getProvider().getSuffix(uuid);
  }

  default TextColor getColor(UUID uuid) {
    return getProvider().getColor(uuid);
  }

  default Component getPrefixComponent(UUID uuid) {
    return getProvider().getPrefixComponent(uuid);
  }

  default Component getSuffixComponent(UUID uuid) {
    return getProvider().getSuffixComponent(uuid);
  }
}
