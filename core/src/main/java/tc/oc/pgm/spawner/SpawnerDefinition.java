package tc.oc.pgm.spawner;

import java.time.Duration;
import java.util.List;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

public class SpawnerDefinition {

  public Region spawnRegion;
  public String id;
  public int count;
  public Region playerRegion;
  public int maxEntities;
  public Duration minDelay, maxDelay, delay;
  public List<SpawnerObject> objects;
  public Filter filter;
}
