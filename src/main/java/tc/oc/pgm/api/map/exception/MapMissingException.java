package tc.oc.pgm.api.map.exception;

import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapSource;

/** Thrown when a {@link MapSource} has gone missing. */
public class MapMissingException extends MapException {

  public MapMissingException(String location, String message) {
    super(location, message);
  }

  public MapMissingException(String location, String message, @Nullable Throwable cause) {
    super(location, message, cause);
  }
}
