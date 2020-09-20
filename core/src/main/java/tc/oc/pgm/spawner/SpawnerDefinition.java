package tc.oc.pgm.spawner;

import java.time.Duration;
import java.util.List;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

@FeatureInfo(name = "spawner")
public class SpawnerDefinition implements FeatureDefinition {

  public final Region spawnRegion;
  public final Region playerRegion;
  public final int maxEntities;
  public final Duration minDelay, maxDelay, delay;
  public final List<Spawnable> objects;
  public final Filter playerFilter;

  public SpawnerDefinition(
      List<Spawnable> objects,
      Region spawnRegion,
      Region playerRegion,
      Filter playerFilter,
      Duration delay,
      Duration minDelay,
      Duration maxDelay,
      int maxEntities) {
    this.spawnRegion = spawnRegion;
    this.playerRegion = playerRegion;
    this.maxEntities = maxEntities;
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.delay = delay;
    this.objects = objects;
    this.playerFilter = playerFilter;
  }
}
