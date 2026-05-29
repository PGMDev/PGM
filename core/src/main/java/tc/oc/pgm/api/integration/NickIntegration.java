package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

public interface NickIntegration {

  /**
   * Get the player nickname
   *
   * @param player The player
   * @return Their nickname
   */
  @Nullable
  String getNick(Player player);

  /**
   * Get the skin for a player per viewer
   *
   * @param player The viewed player
   * @param viewer The viewing player
   * @return The skin shown to a viewer for a player
   */
  @Nullable
  default Skin getPlayerSkin(Player player, Player viewer) {
    return null;
  }
}
