package tc.oc.pgm.api.map;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;

/** A "#hashtag" that describes a {@link MapInfo} feature. */
public final class MapTag implements Comparable<MapTag> {
  private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");
  private static final String SYMBOL = "#";

  private final String id;
  private final Component name;
  private final Component acronym;
  private final boolean gamemode;
  private final boolean auxiliary;

  public MapTag(
      final String id, final String name, final boolean gamemode, final boolean auxiliary) {
    this(id, id, name, gamemode, auxiliary);
  }

  public MapTag(
      final String internalId,
      final String id,
      final String name,
      final boolean gamemode,
      final boolean auxiliary) {
    assertTrue(
        PATTERN.matcher(assertNotNull(id)).matches(), name + " must match " + PATTERN.pattern());
    this.id = id;
    if (gamemode) {
      Gamemode gm = Gamemode.byId(internalId);
      if (gm == null) {
        throw new IllegalArgumentException("Gamemode id " + internalId + " not recognized");
      }
      this.name = text(gm.getFullName());
      this.acronym = text(gm.getAcronym());
    } else {
      this.name = text(name);
      this.acronym = empty();
    }
    this.gamemode = gamemode;
    this.auxiliary = auxiliary;
  }

  /**
   * Get a short id for the tag.
   *
   * @return A short, lowercase id without the "#".
   */
  public String getId() {
    return this.id;
  }

  /**
   * Get a full name for the tag.
   *
   * @return A full name.
   */
  public Component getName() {
    return this.name;
  }

  /**
   * Gets an acronym for the tag.
   *
   * @return An acronym.
   */
  public Component getAcronym() {
    return this.acronym;
  }

  /**
   * Get whether this tag represents a "gamemode."
   *
   * @return If a gamemode.
   */
  public boolean isGamemode() {
    return this.gamemode;
  }

  /**
   * Get whether this tag is an auxiliary feature.
   *
   * @return If an auxiliary feature.
   */
  public boolean isAuxiliary() {
    return this.auxiliary;
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapTag)) return false;
    return this.id.equalsIgnoreCase(((MapTag) obj).id);
  }

  @Override
  public String toString() {
    return SYMBOL + this.id;
  }

  @Override
  public int compareTo(MapTag o) {
    return this.id.compareToIgnoreCase(o.id);
  }
}
