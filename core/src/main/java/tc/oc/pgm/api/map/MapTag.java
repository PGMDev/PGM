package tc.oc.pgm.api.map;

import org.apache.commons.lang3.builder.CompareToBuilder;
import tc.oc.pgm.map.MapTagImpl;

/** A "#hashtag" that describes a {@link MapInfo} feature. */
public interface MapTag extends Comparable<MapTag> {

  /**
   * Get a short id for the tag.
   *
   * @return A short, lowercase id without the "#".
   */
  String getId();

  /**
   * Get a full name for the tag.
   *
   * @return A full name.
   */
  String getName();

  /**
   * Get whether this tag represents a "gamemode."
   *
   * @return If a gamemode.
   */
  boolean isGamemode();

  /**
   * Get whether this tag is an auxiliary feature.
   *
   * @return If an auxiliary feature.
   */
  boolean isAuxiliary();

  @Override
  default int compareTo(MapTag o) {
    return new CompareToBuilder()
        .append(isGamemode(), o.isGamemode())
        .append(isAuxiliary(), o.isAuxiliary())
        .append(getId(), o.getId())
        .build();
  }

  static MapTag create(String id, String name, boolean gamemode, boolean auxiliary) {
    return new MapTagImpl(id, name, gamemode, auxiliary);
  }
}
