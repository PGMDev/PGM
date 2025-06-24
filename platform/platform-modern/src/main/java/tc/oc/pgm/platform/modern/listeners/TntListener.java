package tc.oc.pgm.platform.modern.listeners;

import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import tc.oc.pgm.util.event.EventUtil;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;

public class TntListener implements Listener {

  private TNTPrimeEvent lastEvent;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTntPrime(TNTPrimeEvent event) {
    lastEvent = event;
  }

  @EventHandler
  public void onEntitySpawn(EntitySpawnEvent event) {
    var primeEvent = lastEvent;
    lastEvent = null; // Always cleanup

    if (primeEvent == null || event.isCancelled()) return;
    if (!(event.getEntity() instanceof TNTPrimed tnt)) return;
    if (!tnt.getLocation().getBlock().equals(primeEvent.getBlock())) return;

    ExplosionPrimeEvent pgmEvent;
    if (primeEvent.getPrimingEntity() != null) {
      pgmEvent = new ExplosionPrimeByEntityEvent(tnt, primeEvent.getPrimingEntity());
    } else {
      pgmEvent = new ExplosionPrimeEvent(tnt);
    }
    EventUtil.handleCall(pgmEvent, event);
    tnt.setYield(pgmEvent.getRadius());
    tnt.setIsIncendiary(pgmEvent.getFire());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTntSpawn(EntitySpawnEvent event) {
    if (!(event.getEntity() instanceof TNTPrimed tnt)) return;
    // Ensure self tnt damage isn't negated due to friendly fire being off
    ((CraftTNTPrimed) tnt).getHandle().owner = null;
  }
}
