package tc.oc.pgm.spawner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Location;
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
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;

public class Spawner implements Listener, Tickable {

  private final SpawnerDefinition definition;
  private final Match match;
  private final Logger logger;

  private long lastTick;

  private int spawnedEntities;
  private List<Player> trackedPlayers = new ArrayList<>();

  private static final Random RANDOM = new Random();
  private static long generatedDelay;

  public Spawner(SpawnerDefinition definition, Match match, Logger logger) {
    this.definition = definition;
    this.match = match;
    this.logger = logger;

    this.lastTick = match.getTick().tick;
    generateDelay();
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!canSpawn()) {
      return;
    }
    if (match.getTick().tick - lastTick >= generatedDelay) {
      for (SpawnerObject object : definition.objects) {
        Vector randomSpawnLoc = definition.spawnRegion.getRandom(RANDOM);
        object.spawn(
            new Location(
                match.getWorld(),
                randomSpawnLoc.getX(),
                randomSpawnLoc.getY(),
                randomSpawnLoc.getZ()));
        if (object.isTracked()) {
          spawnedEntities = spawnedEntities + object.spawnCount();
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
              (RANDOM.nextDouble() * (maxDelay - minDelay)
                  + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }
  }

  private boolean canSpawn() {
    return spawnedEntities < definition.maxEntities && trackedPlayers.size() != 0;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    if (event.getEntity().getMetadata(SpawnerModule.METADATA_KEY, PGM.get()) != null) {
      spawnedEntities--;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    if (event.getEntity().getMetadata(SpawnerModule.METADATA_KEY, PGM.get()) != null) {
      spawnedEntities = spawnedEntities - event.getEntity().getItemStack().getAmount();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    if (event.getItem().getMetadata(SpawnerModule.METADATA_KEY, PGM.get()) != null) {
      spawnedEntities = spawnedEntities - event.getItem().getItemStack().getAmount();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(CoarsePlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (match.getPlayer(player).isObserving()) {
      return;
    }
    if (definition.playerRegion.contains(event.getPlayer()) && !trackedPlayers.contains(player)) {
      trackedPlayers.add(player);
    } else if (!definition.playerRegion.contains(event.getPlayer())) {
      trackedPlayers.remove(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    trackedPlayers.remove(event.getPlayer());
  }
}
