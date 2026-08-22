package tc.oc.pgm.api.integration;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

/**
 * Nickname policy integration: which players are nicked, what their nicknames are, who can or
 * cannot see through their nicknames, and what skin is shown to players who cannot see through the
 * nickname, beyond friends and those with the {@code pgm.staff} permission node.
 *
 * <p>PGM only reads this when something tells it to, so an integration must fire a
 * {@link tc.oc.pgm.api.event.NameDecorationChangeEvent} after a player's nickname or disguise skin
 * changes. Showing the disguise to other players is the integration's own responsibility.
 */
public interface NickIntegration {

  /**
   * Get a player's nickname
   *
   * @param player The player
   * @return Their nickname, or null if they are not nicked
   */
  @Nullable
  String getNick(Player player);

  /**
   * Get a nicknamed player's disguise skin
   *
   * @param player The nicknamed player
   * @return Their disguise skin, or null for the player's original skin
   */
  @Nullable
  default Skin getDisguiseSkin(Player player) {
    return null;
  }

  /**
   * Conditions under which a viewing player may be able to see through a player's disguise, beyond
   * friends of this player or those with the {@code pgm.staff} permission node.
   *
   * @param player The disguised player
   * @param viewer The viewer
   * @return True to show this viewer the real name and skin
   */
  default boolean canRevealDisguise(Player player, CommandSender viewer) {
    return false;
  }
}
