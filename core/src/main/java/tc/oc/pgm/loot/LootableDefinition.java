package tc.oc.pgm.loot;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "lootable")
public class LootableDefinition extends SelfIdentifyingFeatureDefinition {
  public final List<Loot> lootableItems;
  public final List<Any> anyLootables;
  public final List<Maybe> maybeLootables;
  public final Cache cache;
  public final Filter filter;
  public final Duration refillInterval;
  public final boolean refillClear;

  public LootableDefinition(
      String id,
      List<Loot> lootableItems,
      List<Any> anyLootables,
      List<Maybe> maybeLootables,
      Cache cache,
      Filter filter,
      // refill-trigger
      Duration refillInterval,
      boolean refillClear) {
    super(id);
    this.lootableItems = lootableItems;
    this.anyLootables = anyLootables;
    this.maybeLootables = maybeLootables;
    this.cache = cache;
    this.filter = filter;
    this.refillInterval = refillInterval;
    this.refillClear = refillClear;
  }

  @Override
  protected String getDefaultId() {
    return super.makeDefaultId();
  }

  public Cache getCache() {
    return cache;
  }

  public static String makeDefaultId(@Nullable String name, AtomicInteger serial) {
    return "--"
        + makeTypeName(LootableDefinition.class)
        + "-"
        + (name != null ? makeId(name) : serial.getAndIncrement());
  }
}
