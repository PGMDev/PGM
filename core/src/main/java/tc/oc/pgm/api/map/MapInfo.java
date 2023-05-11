package tc.oc.pgm.api.map;

import java.time.LocalDate;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextTranslations;

/** Essential information about a map. */
public interface MapInfo extends Comparable<MapInfo>, Cloneable {

  /**
   * Get a unique id for the map.
   *
   * @return A unique id.
   */
  String getId();

  /**
   * The map variant this info represents
   *
   * @return A variant for the map, if any.
   */
  @Nullable
  String getVariant();

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
   * @return A name, alphanumeric with spaces allowed.
   */
  String getName();

  /**
   * Get the maps' name, but normalized to standard english characters and lower case.
   *
   * @return The map's name, lowercase with spaces allowed.
   */
  String getNormalizedName();

  /**
   * Gets a styled map name.
   *
   * @param style A style.
   * @return A styled map name.
   */
  Component getStyledName(MapNameStyle style);

  @Deprecated
  default String getStyledNameLegacy(MapNameStyle style, @Nullable CommandSender sender) {
    return TextTranslations.translateLegacy(getStyledName(style), sender);
  }

  /**
   * Get a short, human-readable description of the map's objective.
   *
   * @return A description.
   */
  String getDescription();

  /**
   * Get the creation date of the map.
   *
   * @return The creation date.
   */
  LocalDate getCreated();

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
   * Get a collection of "hash tags" used to describe the map.
   *
   * @return A collection of tags.
   */
  Collection<MapTag> getTags();

  /**
   * Get a {@link Component} that represents this map's custom game title.
   *
   * @return Returns the defined gamemode title, empty if not defined.
   */
  Component getGamemode();

  /**
   * Get a {@link Collection<Gamemode>} that represents this map's gamemodes.
   *
   * @return A Collection of gamemodes if defined or null.
   */
  Collection<Gamemode> getGamemodes();

  /**
   * Get the maximum number of players that can participate on each team.
   *
   * @return Maximum number of players on each team.
   */
  Collection<Integer> getMaxPlayers();

  /**
   * Get the {@link WorldInfo} that describes how to load the map.
   *
   * @return The {@link WorldInfo}.
   */
  WorldInfo getWorld();

  /**
   * Get the {@link Phase} for the map.
   *
   * @return The {@link Phase}.
   */
  Phase getPhase();

  /**
   * Get whether friendly fire should be on or off.
   *
   * @return True if friendly fire is on.
   */
  boolean getFriendlyFire();

  /**
   * Get a {@link MapSource} to access the maps's files.
   *
   * @return A {@link MapSource}.
   */
  MapSource getSource();

  /**
   * Get the {@link MapContext} for this map, it may be null if the map unloaded
   *
   * @return A {@link MapContext} for this map, or null if unloaded.
   */
  @Nullable
  MapContext getContext();

  @Override
  default int compareTo(MapInfo o) {
    return getId().compareTo(o.getId());
  }
}
