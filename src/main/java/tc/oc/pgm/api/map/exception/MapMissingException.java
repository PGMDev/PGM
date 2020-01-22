package tc.oc.pgm.api.map.exception;

import javax.annotation.Nullable;

/** Thrown when a map's files have gone missing. */
public class MapMissingException extends MapException {

  public MapMissingException(String location, String message) {
    super(location, message);
  }

  public MapMissingException(String location, String message, @Nullable Throwable cause) {
    super(location, message, cause);
  }
}
