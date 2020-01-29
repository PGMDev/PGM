package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Pattern;
import tc.oc.pgm.api.map.MapTag;

public class MapTagImpl implements MapTag {

  private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");
  private static final String SYMBOL = "#";

  private final String id;
  private final String name;
  private final boolean gamemode;
  private final boolean auxiliary;

  public MapTagImpl(String id, String name, boolean gamemode, boolean auxiliary) {
    checkArgument(
        PATTERN.matcher(checkNotNull(id)).matches(), name + " must match " + PATTERN.pattern());
    this.id = id;
    this.name = checkNotNull(name);
    this.gamemode = gamemode;
    this.auxiliary = auxiliary;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isGamemode() {
    return gamemode;
  }

  @Override
  public boolean isAuxiliary() {
    return auxiliary;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapTag)) return false;
    return getId().equalsIgnoreCase(((MapTag) obj).getId());
  }

  @Override
  public String toString() {
    return SYMBOL + getId();
  }
}
