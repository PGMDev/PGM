package tc.oc.pgm.api.match;

import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

/** A router that determines which {@link Match}es a player can join. */
public interface MatchRouter {

  /**
   * Get all the {@link Match}es a player is allowed to join.
   *
   * @param player The player.
   * @param options All {@link Match}es eligible to be played.
   * @return A collection of {@link Match}es or {@code null} if indifferent.
   */
  @Nullable
  default Collection<Match> getMatches(Player player, Collection<Match> options) {
    return null;
  }

  /**
   * Get which {@link Match} a player should join on login.
   *
   * @param event The login event.
   * @param options All {@link Match}es eligible to be played.
   * @return A {@link Match} or {@code null} if indifferent.
   */
  @Nullable
  default Match onLogin(PlayerLoginEvent event, Collection<Match> options) {
    return null;
  }

  /**
   * Notification when a player switches from one {@link Match} to another.
   *
   * @param event The switch event.
   */
  default void onSwitch(PlayerJoinMatchEvent event) {}

  /**
   * Notification when a player quits the server.
   *
   * @param event The quit event.
   */
  default void onQuit(PlayerQuitEvent event) {}
}
