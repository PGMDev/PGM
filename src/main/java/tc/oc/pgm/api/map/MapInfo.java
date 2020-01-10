package tc.oc.pgm.api.map;

import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.Difficulty;
import tc.oc.util.SemanticVersion;

/**
 * Essential information about a map.
 *
 * <p>Unlike {@link MapContext} that should be garbage-collected after match load, {@link MapInfo}
 * should stay in-memory after loading so players can easily list or search for maps they wish to
 * play.
 */
public interface MapInfo extends Comparable<MapInfo> {

  /**
   * Get the unique id of the map.
   *
   * <p>When another {@link MapInfo} shares the same id, only the most recent {@link #getVersion()}
   * should be shown.
   *
   * @return A unique id.
   */
  String getId();

  /**
   * Get the proto of the map {@link org.jdom2.Document}.
   *
   * @return The proto.
   */
  SemanticVersion getProto();

  /**
   * Get the version of the map.
   *
   * @return The version.
   */
  SemanticVersion getVersion();

  /**
   * Get a unique, human-readable name of the map.
   *
   * @return A name, alphanumeric with spaces are allowed.
   */
  String getName();

  /**
   * Get the genre of "game mode" of the map.
   *
   * <p>Used to override the default sidebar title.
   *
   * @return The genre, or {@code null} to auto-detect.
   */
  @Nullable
  String getGenre();

  /**
   * Get a short human-readable description of the map's objective.
   *
   * @return A description.
   */
  String getDescription();

  /**
   * Get the {@link Contributor}s that contributed significantly to the map.
   *
   * <p>There must be at least 1 author.
   *
   * @return The authors.
   */
  Collection<Contributor> getAuthors();

  /**
   * Get the {@link Contributor}s that helped contribute to the map.
   *
   * @return The contributors.
   */
  Collection<Contributor> getContributors();

  /**
   * Get any special rules that participants must follow for the map.
   *
   * <p>As a rule of thumb, try to keep extra rules to a minimum. From experience, it is often
   * difficult to enforce these rules.
   *
   * @return A collection of human-readable rules.
   */
  Collection<String> getRules();

  /**
   * Get the {@link Difficulty} level that should be set for the map.
   *
   * <p>If not set, should default to the {@link org.bukkit.World#getDifficulty()} of the first
   * loaded world on the server.
   *
   * @return The difficulty, or {@code null} for auto-detect.
   */
  @Nullable
  Difficulty getDifficulty();

  /**
   * Get the maximum number of players that can participate on each team.
   *
   * <p>For free-for-all matches, this should be left empty.
   *
   * <p>Sum of all the limits should equal {@link #getPlayerLimit()}.
   *
   * @return Maximum number of participants on each team.
   */
  Collection<Integer> getTeamLimits();

  /**
   * Get the maximum number of players that can participate in the map.
   *
   * <p>Should be capped at {@link org.bukkit.Bukkit#getMaxPlayers()}.
   *
   * @return Maximum number of participants.
   */
  int getPlayerLimit();
}
