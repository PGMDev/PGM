package tc.oc.pgm.api.map;

import java.text.Normalizer;
import java.util.Collection;
import javax.annotation.Nullable;
import tc.oc.util.Version;

/** Essential information about a map. */
public interface MapInfo extends Comparable<MapInfo>, Cloneable {

  /**
   * Get a unique id for the map.
   *
   * @return A unique id.
   */
  String getId();

  /**
   * Get the proto of the map's {@link org.jdom2.Document}.
   *
   * @see MapProtos
   * @return The proto.
   */
  Version getProto();

  /**
   * Get the version of the map.
   *
   * @return The version.
   */
  Version getVersion();

  /**
   * Get a unique, human-readable name for the map.
   *
   * @return A name, alphanumeric with spaces are allowed.
   */
  String getName();

  /**
   * Get a short, human-readable description of the map's objective.
   *
   * @return A description.
   */
  String getDescription();

  /**
   * Get all {@link Contributor}s that contributed significantly to the map.
   *
   * <p>There must be at least 1 author.
   *
   * @return The authors.
   */
  Collection<Contributor> getAuthors();

  /**
   * Get all {@link Contributor}s that helped contribute to the map.
   *
   * @return The contributors.
   */
  Collection<Contributor> getContributors();

  /**
   * Get any special rules that players must follow for the map.
   *
   * <p>Keep these to a minimum, it is often difficult to enforce these rules.
   *
   * @return A collection of rules.
   */
  Collection<String> getRules();

  /**
   * Get the {@link org.bukkit.Difficulty#ordinal()} level for the map.
   *
   * @return The difficulty level.
   */
  int getDifficulty();

  /**
   * Get the genre or "game mode" for the map.
   *
   * <p>Used to override the default sidebar title.
   *
   * @return The genre.
   */
  String getGenre();

  /**
   * Get a collection of "hash tags" used to describe the map.
   *
   * @return A collection of tags.
   */
  Collection<String> getTags();

  /**
   * Get the maximum number of players that can participate on each team.
   *
   * @return Maximum number of players on each team.
   */
  Collection<Integer> getMaxPlayers();

  MapInfo clone();

  @Override
  default int compareTo(MapInfo o) {
    return getId().compareTo(o.getId());
  }

  static String normalizeName(@Nullable String idOrName) {
    return idOrName == null
        ? ""
        : Normalizer.normalize(idOrName, Normalizer.Form.NFD)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
  }
}
