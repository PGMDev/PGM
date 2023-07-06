package tc.oc.pgm.tntrender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(value = MatchScope.LOADED)
public class TNTRenderMatchModule implements MatchModule, Listener {

  private static final double MAX_DISTANCE = Math.pow(64d, 2);

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

  @EventHandler
  public void onTntSpawn(ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed)
      entities.add(new PrimedTnt((TNTPrimed) event.getEntity()));
  }

  @EventHandler
  public void onTntExplode(EntityExplodeEvent event) {
    if (!(event.getEntity() instanceof TNTPrimed)) return;
    Location explosion = event.getLocation();
    for (MatchPlayer player : match.getPlayers()) {
      if (explosion.distanceSquared(player.getBukkit().getLocation()) >= MAX_DISTANCE) {
        NMSHacks.showExplosionParticle(explosion, player.getBukkit());
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
      if (entity.isDead()) {
        for (MatchPlayer viewer : viewers) {
          NMSHacks.sendBlockChange(currentLocation, viewer.getBukkit(), null);
        }
        return true;
      }

      this.lastLocation = currentLocation;
      this.currentLocation = toBlockLocation(entity.getLocation());
      this.moved = !currentLocation.equals(lastLocation);

      for (MatchPlayer player : match.getPlayers()) {
        updatePlayer(player);
      }
      return false;
    }

    private void updatePlayer(MatchPlayer player) {
      if (currentLocation.distanceSquared(player.getBukkit().getLocation()) >= MAX_DISTANCE) {
        if (viewers.add(player)) {
          NMSHacks.sendBlockChange(currentLocation, player.getBukkit(), Material.TNT);
        } else if (moved) {
          NMSHacks.sendBlockChange(lastLocation, player.getBukkit(), null);
          NMSHacks.sendBlockChange(currentLocation, player.getBukkit(), Material.TNT);
        }
      } else if (viewers.remove(player)) {
        NMSHacks.sendBlockChange(lastLocation, player.getBukkit(), null);
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
