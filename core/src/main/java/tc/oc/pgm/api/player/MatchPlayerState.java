package tc.oc.pgm.api.player;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.Named;

/**
 * Represents an immutable "snapshot" view of a {@link tc.oc.pgm.api.player.MatchPlayer} in time.
 */
public interface MatchPlayerState extends Audience, Named {

  /**
   * Get the {@link Match} of the {@link MatchPlayerState}.
   *
   * @return The {@link Match}.
   */
  Match getMatch();

  /**
   * Get the {@link Party} of the {@link MatchPlayerState}.
   *
   * @return The {@link Party}.
   */
  Party getParty();

  /**
   * Get the unique identifier for the {@link MatchPlayerState}.
   *
   * @return The unique identifier.
   */
  UUID getId();

  /**
   * Get the {@link Location} of the {@link tc.oc.pgm.api.player.MatchPlayer} at the snapshot time.
   *
   * @return The last known {@link Location}.
   */
  Location getLocation();

  /**
   * Get the current {@link tc.oc.pgm.api.player.MatchPlayer} if they are online and their {@link
   * Party} is the same.
   *
   * @return The current {@link tc.oc.pgm.api.player.MatchPlayer}.
   */
  Optional<MatchPlayer> getPlayer();

  /**
   * Get whether the {@link MatchPlayerState} represents the given {@link
   * tc.oc.pgm.api.player.MatchPlayer}.
   *
   * @param player The {@link tc.oc.pgm.api.player.MatchPlayer} to check.
   * @return Whether the {@link MatchPlayerState} is from the {@link
   *     tc.oc.pgm.api.player.MatchPlayer}.
   */
  default boolean isPlayer(MatchPlayer player) {
    return getPlayer().map(player::equals).orElse(false);
  }
}
