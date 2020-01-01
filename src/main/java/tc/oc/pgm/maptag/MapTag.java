package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class MapTag implements Comparable<MapTag> {
  public static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");
  public static final char SYMBOL = '#';

  private final String name;

  protected MapTag(String name) {
    name = checkNotNull(name).toLowerCase(Locale.ROOT);
    checkArgument(PATTERN.matcher(name).matches(), name + " must match " + PATTERN.pattern());
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MapTag mapTag = (MapTag) o;
    return Objects.equals(name, mapTag.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public int compareTo(MapTag o) {
    return name.compareTo(o.name);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return SYMBOL + this.name;
  }

  public static MapTag forName(String name) {
    return new MapTag(name);
  }
}
