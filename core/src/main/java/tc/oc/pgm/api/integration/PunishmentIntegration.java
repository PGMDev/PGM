package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface PunishmentIntegration {

  /**
   * Get whether the player is muted
   *
   * @param player - The player
   * @return true if muted, false if not
   */
  boolean isMuted(Player player);

  /**
   * Get the reason why a player might be muted
   *
   * @param player - The player
   * @return A String reason or null if not muted
   */
  @Nullable
  String getMuteReason(Player player);
}
