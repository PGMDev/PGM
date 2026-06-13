package tc.oc.pgm.pickup;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.kits.Kit;

@NullMarked
@FeatureInfo(name = "pickup")
public class PickupDefinition extends SelfIdentifyingFeatureDefinition {

  private final @Nullable String name;
  private final SpawnableEntity appearance;
  private final Filter spawnFilter;
  private final Filter pickupFilter;
  private final Region region;
  private final Kit kit;
  private final Duration respawn;
  private final Duration cooldown;
  private final boolean effects;
  private final boolean sounds;

  public PickupDefinition(
      @Nullable String id,
      @Nullable String name,
      SpawnableEntity appearance,
      Filter spawnFilter,
      Filter pickupFilter,
      Region region,
      Kit kit,
      Duration respawn,
      Duration cooldown,
      boolean effects,
      boolean sounds) {
    super(id);
    this.name = name;
    this.appearance = appearance;
    this.spawnFilter = spawnFilter;
    this.pickupFilter = pickupFilter;
    this.region = region;
    this.kit = kit;
    this.respawn = respawn;
    this.cooldown = cooldown;
    this.effects = effects;
    this.sounds = sounds;
  }

  public @Nullable String getName() {
    return name;
  }

  public SpawnableEntity getAppearance() {
    return appearance;
  }

  public Filter getSpawnFilter() {
    return spawnFilter;
  }

  public Filter getPickupFilter() {
    return pickupFilter;
  }

  public Region getRegion() {
    return region;
  }

  public Kit getKit() {
    return kit;
  }

  public Duration getRespawn() {
    return respawn;
  }

  public Duration getCooldown() {
    return cooldown;
  }

  public boolean hasEffects() {
    return effects;
  }

  public boolean hasSounds() {
    return sounds;
  }
}
