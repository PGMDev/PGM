package tc.oc.pgm.spawner;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

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
            definition.spawnRegion.getRandom(match.getRandom()).toLocation(match.getWorld());
        spawnable.spawn(location, match);
        match.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0, 0.15f, 0, 0, 40, 64);

        if (spawnable.isTracked()) {
          spawnedEntities = spawnedEntities + spawnable.getSpawnCount();
        }
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
      currentDelay =
          (long)
              (match.getRandom().nextDouble() * (maxDelay - minDelay)
                  + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }
    lastTick = match.getTick().tick;
  }

  private boolean canSpawn() {
    if (spawnedEntities >= definition.maxEntities || players.isEmpty()) return false;
    for (MatchPlayer player : players.values()) {
      if (definition.playerFilter.query(player.getQuery()).isAllowed()) return true;
    }
    return false;
  }

  private boolean isTracked(Entity entity) {
    return entity.getMetadata(METADATA_KEY, PGM.get()) != null;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    if (isTracked(event.getEntity())) spawnedEntities--;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    if (isTracked(event.getEntity()))
      spawnedEntities -= event.getEntity().getItemStack().getAmount();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    if (isTracked(event.getItem())) spawnedEntities -= event.getItem().getItemStack().getAmount();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(CoarsePlayerMoveEvent event) {
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
  }
}
