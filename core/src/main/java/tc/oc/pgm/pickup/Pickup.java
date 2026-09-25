package tc.oc.pgm.pickup;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.VectorUtils;
import tc.oc.pgm.util.bukkit.Effects.PickupEffect;

@NullMarked
public class Pickup implements Feature<PickupDefinition> {

  private static final double PICKUP_RADIUS = 0.5;
  private static final double BOX_TO_SPHERE = 0.5;

  private final Match match;
  private final PickupDefinition definition;
  private final Map<UUID, Long> cooldowns = new HashMap<>();

  private @Nullable Entity entity;
  private @Nullable Location location;
  private @Nullable Long spawnAtTick;

  public Pickup(Match match, PickupDefinition definition) {
    this.match = match;
    this.definition = definition;
    this.spawnAtTick = 0L;
  }

  @Override
  public @Nullable String getId() {
    return definition.getId();
  }

  @Override
  public PickupDefinition getDefinition() {
    return definition;
  }

  private void spawn() {
    Location location = definition.getRegion().getRandomLoc(match);

    Entity entity = definition.getAppearance().spawn(location);
    NMS_HACKS.freezeEntity(entity);
    NMS_HACKS.setupPickup(entity);

    if (definition.getName() != null) {
      entity.setCustomName(definition.getName());
      entity.setCustomNameVisible(true);
    }

    this.entity = entity;
    this.location = location;
    this.spawnAtTick = null;
    playEffects(entity, PickupEffect.SPAWN);
  }

  private void despawn(boolean effects, @Nullable Long spawnAt) {
    Entity entity = this.entity;
    if (entity != null) {
      if (effects) playEffects(entity, PickupEffect.DESPAWN);
      entity.remove();
      this.entity = null;
      this.location = null;
    }
    this.spawnAtTick = spawnAt;
  }

  private @Nullable Long respawnTick() {
    Duration respawn = definition.getRespawn();
    if (TimeUtils.isInfinite(respawn)) return null;
    return match.getTick().tick + TimeUtils.toTicks(respawn);
  }

  private void playEffects(Entity entity, PickupEffect effect) {
    if (definition.hasEffects()) EFFECTS.pickupEffect(entity, effect);
    if (definition.hasSounds()) match.playSound(effect.sound(), entity.getLocation());
  }

  void tick(Match match, Tick tick) {
    Entity entity = this.entity;
    if (entity != null) {
      if (!entity.isValid()) {
        despawn(false, respawnTick());
      } else if (definition.getSpawnFilter().query(match).isDenied()) {
        despawn(true, tick.tick);
      } else {
        NMS_HACKS.tickFrozenEntity(entity, assertNotNull(this.location));
      }
    } else if (spawnAtTick != null
        && tick.tick >= spawnAtTick
        && definition.getSpawnFilter().query(match).isAllowed()) {
      spawn();
    }
  }

  boolean canPickup(MatchPlayer player, Location from, Location to) {
    Entity entity = this.entity;
    return entity != null
        && player.canInteract()
        && !isOnCooldown(player.getId())
        && isMovementInEntityRange(from, to, entity)
        && definition.getPickupFilter().query(player).isAllowed();
  }

  private boolean isMovementInEntityRange(Location from, Location to, Entity entity) {
    if (from.getWorld() == null || to.getWorld() == null) return false;
    if (!from.getWorld().equals(entity.getWorld()) || !to.getWorld().equals(entity.getWorld())) {
      return false;
    }

    Vector center = NMS_HACKS.getBoundingBoxCenter(entity);
    double radius =
        NMS_HACKS.getBoundingBoxSize(entity).multiply(BOX_TO_SPHERE).length() + PICKUP_RADIUS;
    return VectorUtils.segmentIntersectsSphere(from, to, center, radius);
  }

  void pickup(MatchPlayer player) {
    Entity entity = this.entity;
    if (entity == null) return;

    long now = match.getTick().tick;
    cooldowns.values().removeIf(until -> until <= now);
    cooldowns.put(player.getId(), now + TimeUtils.toTicks(definition.getCooldown()));

    if (definition.getRespawn().isZero()) {
      playEffects(entity, PickupEffect.PICKUP);
    } else {
      despawn(true, respawnTick());
    }

    player.applyKit(definition.getKit(), false);
  }

  boolean isEntity(Entity entity) {
    return entity.equals(this.entity);
  }

  void reset() {
    despawn(true, match.getTick().tick);
  }

  void removeCooldown(UUID uuid) {
    cooldowns.remove(uuid);
  }

  private boolean isOnCooldown(UUID uuid) {
    Long until = cooldowns.get(uuid);
    return until != null && until > match.getTick().tick;
  }

  void unload() {
    despawn(false, null);
    cooldowns.clear();
  }
}
