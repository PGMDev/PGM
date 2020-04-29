package tc.oc.pgm.spawner;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class Spawner implements Feature<SpawnerDefinition>, Listener, Tickable {

  private final SpawnerDefinition definition;
  private final Match match;

  private long lastTick;

  private int spawnedEntities;
  private OnlinePlayerMapAdapter<MatchPlayer> trackedPlayers;

  public static final String METADATA_KEY = "PGM_SPAWNER_OBJECT";

  private static long generatedDelay;

  public Spawner(SpawnerDefinition definition, Match match) {
    this.definition = definition;
    this.match = match;

    this.lastTick = match.getTick().tick;
    this.trackedPlayers = new OnlinePlayerMapAdapter<>(PGM.get());
    generateDelay();
  }

  @Override
  public String getId() {
    return definition.id;
  }

  @Override
  public SpawnerDefinition getDefinition() {
    return definition;
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!canSpawn()) {
      return;
    }
    if (match.getTick().tick - lastTick >= generatedDelay) {
      for (Spawnable spawnable : definition.objects) {
        Vector randomSpawnLoc = definition.spawnRegion.getRandom(match.getRandom());
        spawnable.spawn(
            new Location(
                match.getWorld(),
                randomSpawnLoc.getX(),
                randomSpawnLoc.getY(),
                randomSpawnLoc.getZ()),
            match);
        if (spawnable.isTracked()) {
          spawnedEntities = spawnedEntities + spawnable.getSpawnCount();
        }
      }
      generateDelay();
      lastTick = match.getTick().tick;
    }
  }

  private void generateDelay() {
    if (definition.minDelay == definition.maxDelay) {
      generatedDelay = TimeUtils.toTicks(definition.delay);
    } else {
      long maxDelay = TimeUtils.toTicks(definition.maxDelay);
      long minDelay = TimeUtils.toTicks(definition.minDelay);
      generatedDelay =
          (long)
              (match.getRandom().nextDouble() * (maxDelay - minDelay)
                  + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }
  }

  private boolean canSpawn() {
    if (spawnedEntities >= definition.maxEntities || trackedPlayers.isEmpty()) {
      return false;
    }
    for (MatchPlayer p : trackedPlayers.values()) {
      if (definition.playerFilter.query(p.getQuery()).isAllowed()) {
        return true;
      }
    }
    return false;
  }

  private boolean isTrackedEntity(Entity entity) {
    return entity.getMetadata(METADATA_KEY, PGM.get()) != null;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    if (isTrackedEntity(event.getEntity())) {
      spawnedEntities--;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    if (isTrackedEntity(event.getEntity())) {
      spawnedEntities = spawnedEntities - event.getEntity().getItemStack().getAmount();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    if (isTrackedEntity(event.getItem())) {
      spawnedEntities = spawnedEntities - event.getItem().getItemStack().getAmount();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(CoarsePlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (match.getPlayer(player).isObserving()) {
      return;
    }
    if (definition.playerRegion.contains(event.getPlayer()) && trackedPlayers.get(player) == null) {
      trackedPlayers.put(player, match.getPlayer(player));
    } else if (!definition.playerRegion.contains(event.getPlayer())) {
      trackedPlayers.remove(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    trackedPlayers.remove(event.getPlayer());
  }
}
