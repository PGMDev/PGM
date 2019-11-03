package tc.oc.pgm.api.party;

import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

/** A {@link Party} that can exclusively win a {@link Match}. */
public interface Competitor extends Party {

  /**
   * Get the constant, unique identifier for the {@link Competitor}
   *
   * @return The unique identifier.
   */
  String getId();

  /**
   * Get the {@link NameTagVisibility} for {@link MatchPlayer}s.
   *
   * @return The {@link NameTagVisibility}.
   */
  NameTagVisibility getNameTagVisibility();
}
