package tc.oc.pgm.api.map;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/** Additional information about a {@link MapInfo} provided by a {@link MapModule}. */
public interface MapInfoExtra {

  /**
   * Get the genre of "game mode" of the map.
   *
   * <p>Used to override the default sidebar title.
   *
   * @return The genre, or {@code null} to auto-detect.
   */
  @Nullable
  default String getGenre() {
    return null;
  }

  /**
   * Get a collection of "hash tags" used to describe the map.
   *
   * @return A collection of tags or {@code null} for none.
   */
  default Collection<String> getTags() {
    return Collections.emptyList();
  }

  /**
   * Get the maximum number of players that can participate on each team.
   *
   * <p>For free-for-all matches, this should be {@code null}.
   *
   * <p>Sum of all the limits should equal {@link #getPlayerLimit()}.
   *
   * @return Maximum number of participants on each team or {@code null} for none.
   */
  @Nullable
  default Collection<Integer> getTeamLimits() {
    return null;
  }

  /**
   * Get the maximum number of players that can participate in the map.
   *
   * <p>Should be capped at {@link org.bukkit.Bukkit#getMaxPlayers()}.
   *
   * @return Maximum number of participants or {@code null} for none.
   */
  default int getPlayerLimit() {
    return 0;
  }
}
