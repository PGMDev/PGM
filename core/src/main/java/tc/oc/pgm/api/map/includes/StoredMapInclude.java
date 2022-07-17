package tc.oc.pgm.api.map.includes;

/**
 * A snapshot of {@link MapInclude} info, used to determine when include files have been updated. *
 */
public interface StoredMapInclude {

  /**
   * Gets the unique include id, used to reference a {@link MapInclude}.
   *
   * @return A unique include id
   */
  String getIncludeId();

  /**
   * Gets whether the associated {@link MapInclude} files have changed since last loading.
   *
   * @param time The current system time
   * @return True if given time is newer than last modified time
   */
  boolean hasBeenModified(long time);
}
