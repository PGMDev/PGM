package tc.oc.pgm.api.map.exception;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.factory.MapFactory;

/** Thrown when a {@link MapFactory} cannot parse a map's document. */
public class MapException extends Exception {

  private final String location;
  private final @Nullable MapInfo info;

  public MapException(String location, String message) {
    this(location, message, null);
  }

  public MapException(String location, String message, @Nullable Throwable cause) {
    this(location, null, message, cause);
  }

  public MapException(
      MapSource source, @Nullable MapInfo info, String message, @Nullable Throwable cause) {
    this(source.getId(), info, message, cause);
  }

  public MapException(
      String location, @Nullable MapInfo info, String message, @Nullable Throwable cause) {
    super(assertNotNull(message), cause);
    this.location = assertNotNull(location);
    this.info = info;
  }

  public String getLocation() {
    return location;
  }

  public @Nullable MapInfo getMap() {
    return info;
  }
}
