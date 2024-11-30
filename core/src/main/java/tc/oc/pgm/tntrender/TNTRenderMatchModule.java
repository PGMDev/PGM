package tc.oc.pgm.tntrender;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.event.block.BlockDispenseEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

@ListenerScope(value = MatchScope.LOADED)
public class TNTRenderMatchModule implements MatchModule, Listener {
  private static final Duration AFK_TIME = Duration.ofSeconds(30);
  private static final double MAX_DISTANCE = Math.pow(64d, 2);
  private static final BlockMaterialData TNT = MaterialData.block(Material.TNT);

  private final Match match;
  private final List<PrimedTnt> entities;

  public TNTRenderMatchModule(Match match) {
    this.match = match;
    this.entities = new ArrayList<>();
  }

  @Override
  public void enable() {
    match
        .getExecutor(MatchScope.LOADED)
        .scheduleAtFixedRate(
            () -> entities.removeIf(PrimedTnt::update), 0, 50, TimeUnit.MILLISECONDS);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTntSpawn(ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed)
      entities.add(new PrimedTnt((TNTPrimed) event.getEntity()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDispense(BlockDispenseEntityEvent event) {
    if (event.getEntity() instanceof TNTPrimed)
      entities.add(new PrimedTnt((TNTPrimed) event.getEntity()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTntExplode(EntityExplodeEvent event) {
    if (!(event.getEntity() instanceof TNTPrimed)) return;
    Location explosion = event.getLocation();
    for (MatchPlayer player : match.getPlayers()) {
      if (player.getWorld() != event.getEntity().getWorld()) continue;
      if (explosion.distanceSquared(player.getBukkit().getLocation()) >= MAX_DISTANCE) {
        EFFECTS.explosion(player.getBukkit(), explosion);
      }
    }
  }

  private class PrimedTnt {
    private final TNTPrimed entity;
    private final Set<MatchPlayer> viewers = new HashSet<>();
    private Location lastLocation, currentLocation;
    private boolean moved = false;

    public PrimedTnt(TNTPrimed entity) {
      this.entity = entity;
      this.lastLocation = currentLocation = toBlockLocation(entity.getLocation());
    }

    public boolean update() {
      var currentMaterial = MaterialData.block(currentLocation.getBlock());
      if (entity.isDead()) {
        for (MatchPlayer viewer : viewers) {
          currentMaterial.sendBlockChange(viewer.getBukkit(), currentLocation);
        }
        return true;
      }

      this.lastLocation = currentLocation;
      this.currentLocation = toBlockLocation(entity.getLocation());
      this.moved = !currentLocation.equals(lastLocation);

      for (MatchPlayer player : match.getPlayers()) {
        if (player.getWorld() != entity.getWorld()) continue;
        updatePlayer(player, currentMaterial);
      }
      return false;
    }

    private void updatePlayer(MatchPlayer player, BlockMaterialData md) {
      if (currentLocation.distanceSquared(player.getLocation()) >= MAX_DISTANCE
          && player.isActive(AFK_TIME)) {
        if (viewers.add(player)) {
          TNT.sendBlockChange(player.getBukkit(), currentLocation);
        } else if (moved) {
          md.sendBlockChange(player.getBukkit(), lastLocation);
          TNT.sendBlockChange(player.getBukkit(), currentLocation);
        }
      } else if (viewers.remove(player)) {
        md.sendBlockChange(player.getBukkit(), lastLocation);
      }
    }
  }

  private static Location toBlockLocation(Location location) {
    // Spigot 1.8 doesn't have Location.toBlockLocation()
    Location blockLoc = location.clone();
    blockLoc.setX(blockLoc.getBlockX());
    blockLoc.setY(blockLoc.getBlockY());
    blockLoc.setZ(blockLoc.getBlockZ());
    return blockLoc;
  }
}
