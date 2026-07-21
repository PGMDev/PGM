package tc.oc.pgm.spawner;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.TimeUtils;

@FeatureInfo(name = "spawner")
public class SpawnerDefinition extends SelfIdentifyingFeatureDefinition {
  public final Region spawnRegion;
  public final Region playerRegion;
  public final int maxEntities;
  public final long minDelay, maxDelay;
  public final List<Spawnable> objects;
  public final Filter matchFilter;
  public final Filter playerFilter;

  public SpawnerDefinition(
      String id,
      List<Spawnable> objects,
      Region spawnRegion,
      Region playerRegion,
      Filter matchFilter,
      Filter playerFilter,
      Duration minDelay,
      Duration maxDelay,
      int maxEntities) {
    super(id);
    this.spawnRegion = spawnRegion;
    this.playerRegion = playerRegion;
    this.maxEntities = maxEntities;
    this.minDelay = TimeUtils.toTicks(minDelay);
    this.maxDelay = TimeUtils.toTicks(maxDelay);
    this.objects = objects;
    this.matchFilter = matchFilter;
    this.playerFilter = playerFilter;
  }

  @Override
  protected String getDefaultId() {
    return super.makeDefaultId();
  }

  public static String makeDefaultId(@Nullable String name, AtomicInteger serial) {
    return "--"
        + makeTypeName(SpawnerDefinition.class)
        + "-"
        + (name != null ? makeId(name) : serial.getAndIncrement());
  }
}
