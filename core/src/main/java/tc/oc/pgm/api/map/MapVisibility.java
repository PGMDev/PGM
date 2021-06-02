package tc.oc.pgm.api.map;

/** Holds information about whether a {@link MapInfo} should be viewable * */
public interface MapVisibility {

  /**
   * Get the linked {@link MapInfo}
   *
   * @return A {@link MapInfo}
   */
  MapInfo getMap();

  /**
   * Get whether this map is publicly viewable or not
   *
   * @return if map should be hidden
   */
  boolean isHidden();

  /**
   * Set if the map should be hidden
   *
   * @param hidden Whether map should be hidden or not
   */
  void setHidden(boolean hidden);
}
