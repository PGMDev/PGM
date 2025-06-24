package tc.oc.pgm.api.map;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/** A "#hashtag" that describes a {@link MapInfo} feature. */
public final class MapTag implements Comparable<MapTag> {
  private static final SortedSet<String> TAG_IDS = new TreeSet<>();

  private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");
  private static final String SYMBOL = "#";

  private final String id;
  private final Component name;
  private final @Nullable Gamemode gamemode;
  private final boolean auxiliary;

  public MapTag(String id, String name) {
    this(id, name, null, true);
  }

  public MapTag(String id, Gamemode gm, boolean auxiliary) {
    this(id, gm.getFullName(), gm, auxiliary);
  }

  private MapTag(String id, String name, @Nullable Gamemode gamemode, boolean auxiliary) {
    assertNotNull(id);
    assertTrue(PATTERN.matcher(id).matches(), id + " must match " + PATTERN.pattern());
    TAG_IDS.add(id);
    this.id = id;
    this.name = text(name);
    this.gamemode = gamemode;
    this.auxiliary = auxiliary;
  }

  public static Set<String> getAllTagIds() {
    return Collections.unmodifiableSortedSet(TAG_IDS);
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
   * Get whether this tag represents a "gamemode."
   *
   * @return If the tag is for a gamemode.
   */
  public boolean isGamemode() {
    return this.gamemode != null;
  }

  /** @return the gamemode if this tag represents one, null otherwise. */
  public @Nullable Gamemode getGamemode() {
    return this.gamemode;
  }

  /**
   * Get whether this tag is an auxiliary gamemode, that works as a 2nd level gamemode. Eg: blitz or
   * rage are auxiliary due to wool "and blitz", or deathmatch "and rage".
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
