package tc.oc.pgm.api.player;

import java.util.Collection;
import java.util.UUID;

/** A manager that holds information related to {@link MatchPlayer}s who are vanished. */
public interface VanishManager {

  /**
   * A collection of {@link MatchPlayer} who are online and vanished.
   *
   * @return A collection of {@link MatchPlayer}
   */
  Collection<MatchPlayer> getOnlineVanished();

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

  /** Called when the vanish manager is disabled */
  void disable();
}
