package tc.oc.pgm.api.map.exception;

import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapSource;

/** Thrown when a {@link MapSource} cannot find a map. */
public class MapNotFoundException extends RuntimeException {

  private final @Nullable MapInfo info;

  public MapNotFoundException() {
    this(null, null, null);
  }

  public MapNotFoundException(String message) {
    this(null, message);
  }

  public MapNotFoundException(Throwable cause) {
    this(cause.getMessage(), cause);
  }

  public MapNotFoundException(MapInfo info, String message) {
    this(info, message, null);
  }

  public MapNotFoundException(MapInfo info, String message, Throwable cause) {
    super(message, cause);
    this.info = info;
  }

  public MapNotFoundException(String message, Throwable cause) {
    this(null, message, cause);
  }

  public @Nullable MapInfo getMap() {
    return info;
  }
}
