package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;

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
    return o instanceof MapTag && Objects.equals(name, ((MapTag) o).name);
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

  public Component getComponentName() {
    return new PersonalizedText(toString());
  }

  @Override
  public String toString() {
    return SYMBOL + this.name;
  }

  public static MapTag forName(String name) {
    return new MapTag(name);
  }
}
