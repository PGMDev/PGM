package tc.oc.pgm.spawner;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.controlpoint.RegionPlayerTracker;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.bukkit.MetadataUtils;

public class Spawner implements Listener, Tickable {

  public static final String METADATA_KEY = "spawner";

  private final Match match;
  private final SpawnerDefinition definition;
  private final RegionPlayerTracker playerTracker;
  private final Filter matchFilter;
  private final Filter playerFilter;

  private long canSpawnAt;
  private long spawnedEntities;

  public Spawner(SpawnerDefinition definition, Match match) {
    this.definition = definition;
    this.match = match;
    this.playerTracker = new RegionPlayerTracker(match, definition.playerRegion);
    boolean isMatchFilter = definition.filter.respondsTo(Match.class);
    this.matchFilter = isMatchFilter ? definition.filter : StaticFilter.ALLOW;
    this.playerFilter = isMatchFilter ? StaticFilter.ALLOW : definition.filter;
  }

  public void registerEvents() {
    match.addListener(this, MatchScope.RUNNING);
    match.addTickable(this, MatchScope.RUNNING);
    match.addListener(this.playerTracker, MatchScope.RUNNING);
  }

  public void unregisterEvents() {
    HandlerList.unregisterAll(this);
    HandlerList.unregisterAll(this.playerTracker);
  }

  @Override
  public void tick(Match match, Tick tick) {
    var now = match.getTick().tick;
    if (now < this.canSpawnAt || spawnedEntities >= definition.maxEntities) return;
    if (!matchFilter.response(match) || !anyPlayerAllows()) return;

    for (Spawnable spawnable : definition.objects) {
      var location = definition.spawnRegion.getRandomLoc(match);
      spawnable.spawn(location, match);
      EFFECTS.spawnFlame(match.getWorld(), location);
      spawnedEntities += spawnable.getSpawnCount();
    }
    canSpawnAt = now + calculateDelay();
  }

  private long calculateDelay() {
    long delay = definition.minDelay;
    if (definition.maxDelay != delay)
      delay += (long) ((definition.maxDelay - delay) * match.getRandom().nextDouble());
    return delay;
  }

  private boolean anyPlayerAllows() {
    if (playerFilter == StaticFilter.ALLOW) return !playerTracker.getPlayers().isEmpty();

    for (MatchPlayer player : playerTracker.getPlayers()) {
      if (playerFilter.query(player).isAllowed()) return true;
    }
    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onItemMerge(ItemMergeEvent event) {
    var entityMeta = MetadataUtils.getMetadataValue(event.getEntity(), METADATA_KEY, PGM.get());
    var targetMeta = MetadataUtils.getMetadataValue(event.getTarget(), METADATA_KEY, PGM.get());
    if (entityMeta == null && targetMeta == null) return; // Not spawner related
    if (!Objects.equals(entityMeta, targetMeta)) {
      event.setCancelled(true); // Different spawners, prevent merging
    }
  }

  private void handleEntityRemoveEvent(Entity entity, boolean affectCount) {
    var metadata = MetadataUtils.getMetadataValue(entity, METADATA_KEY, PGM.get());
    if (Objects.equals(definition.getId(), metadata)) {
      entity.removeMetadata(METADATA_KEY, PGM.get());
      if (affectCount) {
        int removedEntities = entity instanceof Item item ? item.getItemStack().getAmount() : 1;
        spawnedEntities = Math.max(0, spawnedEntities - removedEntities);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    handleEntityRemoveEvent(event.getEntity(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    handleEntityRemoveEvent(event.getEntity(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemMergeRecover(ItemMergeEvent event) {
    // Entity merging does not affect count.
    // We do need to remove the meta so the remove from world doesn't subtract.
    handleEntityRemoveEvent(event.getEntity(), false);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityRemove(EntityRemoveFromWorldEvent event) {
    handleEntityRemoveEvent(event.getEntity(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPotionSplash(PotionSplashEvent event) {
    handleEntityRemoveEvent(event.getEntity(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    handleEntityRemoveEvent(event.getItem(), true);
  }
}
