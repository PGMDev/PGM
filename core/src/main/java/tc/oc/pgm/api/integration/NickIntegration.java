package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface NickIntegration {

  /**
   * Get the player nickname
   *
   * @param player - The player
   * @return Their nickname
   */
  @Nullable
  String getNick(Player player);
}
