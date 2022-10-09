package tc.oc.pgm.db;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapActivity;

class MapActivityImpl implements MapActivity {

  private final String poolName;
  private String mapName;
  private boolean active;

  MapActivityImpl(String poolName, @Nullable String mapName, boolean active) {
    this.poolName = assertNotNull(poolName, "map pool name is null");
    this.mapName = mapName;
    this.active = active;
  }

  @Override
  public String getPoolName() {
    return poolName;
  }

  @Override
  public String getMapName() {
    return mapName;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void update(@Nullable String nextMap, boolean active) {
    this.mapName = nextMap;
    this.active = active;
  }

  @Override
  public int hashCode() {
    return poolName.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MapActivity)) return false;
    return getPoolName().equalsIgnoreCase(((MapActivity) o).getPoolName());
  }

  @Override
  public String toString() {
    return poolName + " (map=" + mapName + ", active=" + active + ")";
  }
}
