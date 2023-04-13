package tc.oc.pgm.api.integration;

import java.util.UUID;
import tc.oc.pgm.api.player.MatchPlayer;

public interface VanishIntegration {

  /**
   * Returns whether the matching UUID is vanished
   *
   * @param uuid - UUID to check
   * @return Whether the provided UUID is vanished
   */
  boolean isVanished(UUID uuid);

  /**
   * Set the {@link MatchPlayer}'s vanish status
   *
   * @param player - The target {@link MatchPlayer}
   * @param vanish - Whether the player is vanished or not
   * @param quiet - Whether to broadcast related join/quit messages
   * @return whether the action was successful
   */
  boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet);
}
