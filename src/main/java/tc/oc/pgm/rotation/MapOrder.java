package tc.oc.pgm.rotation;

import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;

/**
 * A provider of {@link MapInfo} ordering order. It is responsible for providing the next map the
 * server should play.
 */
public interface MapOrder {
  /**
   * Returns next map to play, and removes it from the queue.
   *
   * @return The next map to play
   */
  MapInfo popNextMap();

  /**
   * Returns the next map that will be played. Returning a null value is allowed and specifies the
   * map order hasn't picked the next map yet.
   *
   * @return The next map to play, if present.
   */
  MapInfo getNextMap();

  /**
   * Forces a specific map to be played next. The underlying {@link MapOrder} may ignore this, but
   * it is recommended not to.
   *
   * @param map The map to set next
   */
  void setNextMap(MapInfo map);

  /**
   * Notify the {@link MapOrder} that a match just ended, to allow it to run actions before cycle
   * starts.
   *
   * @param match The match that just ended
   */
  default void matchEnded(Match match) {}
}
