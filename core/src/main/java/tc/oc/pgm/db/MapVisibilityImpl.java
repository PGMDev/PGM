package tc.oc.pgm.db;

import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapVisibility;

public class MapVisibilityImpl implements MapVisibility {

  private final MapInfo map;
  private boolean hidden;

  public MapVisibilityImpl(MapInfo map, boolean hidden) {
    this.map = map;
    this.hidden = hidden;
  }

  @Override
  public MapInfo getMap() {
    return map;
  }

  @Override
  public boolean isHidden() {
    return hidden;
  }

  @Override
  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof MapVisibility)) return false;
    return ((MapVisibility) other).getMap().equals(getMap());
  }
}
