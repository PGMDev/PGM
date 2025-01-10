package tc.oc.pgm.spawner;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.Metadatable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class Spawner implements Listener, Tickable {

  public static final String METADATA_KEY = "spawner";

  private final Match match;
  private final SpawnerDefinition definition;
  private final OnlinePlayerMapAdapter<MatchPlayer> players;

  private long lastTick;
  private long currentDelay;
  private long spawnedEntities;

  public Spawner(SpawnerDefinition definition, Match match) {
    this.definition = definition;
    this.match = match;
    this.lastTick = match.getTick().tick;
    this.players = new OnlinePlayerMapAdapter<>(PGM.get());
    calculateDelay();
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!canSpawn()) return;
    if (match.getTick().tick - lastTick >= currentDelay) {
      for (Spawnable spawnable : definition.objects) {
        final Location location =
            definition.spawnRegion.getRandom(match).toLocation(match.getWorld());
        spawnable.spawn(location, match);
        EFFECTS.spawnFlame(match.getWorld(), location);
        spawnedEntities += spawnable.getSpawnCount();
      }
      calculateDelay();
    }
  }

  private void calculateDelay() {
    if (definition.minDelay == definition.maxDelay) {
      currentDelay = TimeUtils.toTicks(definition.delay);
    } else {
      long maxDelay = TimeUtils.toTicks(definition.maxDelay);
      long minDelay = TimeUtils.toTicks(definition.minDelay);
      currentDelay = (long) (match.getRandom().nextDouble() * (maxDelay - minDelay)
          + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }
    lastTick = match.getTick().tick;
  }

  private boolean canSpawn() {
    if (spawnedEntities >= definition.maxEntities || players.isEmpty()) return false;
    for (MatchPlayer player : players.values()) {
      if (definition.playerFilter.query(player).isAllowed()) return true;
    }
    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onItemMerge(ItemMergeEvent event) {
    boolean entityTracked = event.getEntity().hasMetadata(METADATA_KEY);
    boolean targetTracked = event.getTarget().hasMetadata(METADATA_KEY);
    if (!entityTracked && !targetTracked) return; // None affected
    if (entityTracked && targetTracked) {
      String entitySpawnerId =
          MetadataUtils.getMetadataValue(event.getEntity(), METADATA_KEY, PGM.get());
      String targetSpawnerId =
          MetadataUtils.getMetadataValue(event.getTarget(), METADATA_KEY, PGM.get());
      if (Objects.equals(entitySpawnerId, targetSpawnerId)) return; // Same spawner, allow merge
    }
    event.setCancelled(true);
  }

  private void handleEntityRemoveEvent(Metadatable metadatable, int amount) {
    if (metadatable.hasMetadata(METADATA_KEY)) {
      if (Objects.equals(
          MetadataUtils.getMetadataValue(metadatable, METADATA_KEY, PGM.get()),
          definition.getId())) {
        spawnedEntities -= amount;
        spawnedEntities = Math.max(0, spawnedEntities);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    handleEntityRemoveEvent(event.getEntity(), 1);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    handleEntityRemoveEvent(event.getEntity(), event.getEntity().getItemStack().getAmount());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPotionSplash(PotionSplashEvent event) {
    handleEntityRemoveEvent(event.getEntity(), 1);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    handleEntityRemoveEvent(event.getItem(), event.getItem().getItemStack().getAmount());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    final MatchPlayer player = match.getParticipant(event.getPlayer());
    if (player == null) return;
    if (definition.playerRegion.contains(event.getPlayer())) {
      players.putIfAbsent(event.getPlayer(), player);
    } else {
      players.remove(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    this.players.clear();
    this.players.disable();
  }
}
