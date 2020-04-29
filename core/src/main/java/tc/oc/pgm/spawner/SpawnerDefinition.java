package tc.oc.pgm.spawner;

import java.time.Duration;
import java.util.List;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

@FeatureInfo(name = "spawner")
public class SpawnerDefinition implements FeatureDefinition {

  public Region spawnRegion;
  public String id;
  public Region playerRegion;
  public int maxEntities;
  public Duration minDelay, maxDelay, delay;
  public List<Spawnable> objects;
  public Filter playerFilter;
}
