package tc.oc.pgm.api.player;

import app.ashcon.intake.CommandException;
import java.util.List;
import java.util.UUID;

/** A manager that holds information related to {@link MatchPlayer}s who are vanished. */
public interface VanishManager {

  /**
   * A list of {@link MatchPlayer} who are online and vanished.
   *
   * @return A list of {@link MatchPlayer}
   */
  List<MatchPlayer> getOnlineVanished();

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
   * @throws CommandException When player is already vanished/unvanished
   */
  void setVanished(MatchPlayer player, boolean vanish, boolean quiet) throws CommandException;

  /** Called when the vanish manager is disabled */
  void disable();
}
