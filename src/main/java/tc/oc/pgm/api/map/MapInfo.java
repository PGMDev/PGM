package tc.oc.pgm.api.map;

import org.bukkit.Difficulty;
import tc.oc.util.SemanticVersion;

import java.util.Collection;

/** Essential information about a map. */
public interface MapInfo extends Comparable<MapInfo> {

  /**
   * Get the unique id of the map.
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
  Difficulty getDifficulty();

  @Override
  default int compareTo(MapInfo o) {
    return getId().compareTo(o.getId());
  }
}
