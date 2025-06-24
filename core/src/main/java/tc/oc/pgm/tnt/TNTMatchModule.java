package tc.oc.pgm.tnt;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.inventory.InventoryUtils;

@ListenerScope(MatchScope.RUNNING)
public class TNTMatchModule implements MatchModule, Listener {

  private final Match match;
  private final TNTProperties properties;

  public TNTMatchModule(Match match, TNTProperties properties) {
    this.match = match;
    this.properties = properties;
  }

  public TNTProperties getProperties() {
    return properties;
  }

  public int getFuseTicks() {
    assert this.properties.fuse != null;
    return (int) TimeUtils.toTicks(this.properties.fuse);
  }

  private boolean callPrimeEvent(TNTPrimed tnt, @Nullable Entity primer) {
    ExplosionPrimeEvent primeEvent;
    if (primer != null) {
      primeEvent = new ExplosionPrimeByEntityEvent(tnt, primer);
    } else {
      primeEvent = new ExplosionPrimeEvent(tnt);
    }
    match.callEvent(primeEvent);
    if (primeEvent.isCancelled()) {
      tnt.remove();
      return false;
    } else {
      return true;
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void yieldSet(EntityExplodeEvent event) {
    if (this.properties.yield != null && event.getEntity() instanceof TNTPrimed) {
      event.setYield(this.properties.yield);
    }
  }

  private static final Sound FUSE_SOUND = parse(Sound::valueOf, "FUSE", "ENTITY_TNT_PRIMED");

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleInstantActivation(BlockPlaceEvent event) {
    if (this.properties.instantIgnite && event.getBlock().getType() == Material.TNT) {
      World world = event.getBlock().getWorld();
      TNTPrimed tnt = world.spawn(
          event.getBlock().getLocation().clone().add(new Location(world, 0.5, 0.5, 0.5)),
          TNTPrimed.class);

      if (this.properties.fuse != null) {
        tnt.setFuseTicks(this.getFuseTicks());
      }

      if (this.properties.power != null) {
        tnt.setYield(this.properties.power); // Note: not related to EntityExplodeEvent.yield
      }

      if (callPrimeEvent(tnt, event.getPlayer())) {
        event.setCancelled(true); // Allow the block to be placed if priming is cancelled
        world.playSound(tnt.getLocation(), FUSE_SOUND, 1, 1);
        InventoryUtils.consumeItem(event, event.getPlayer());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void setCustomProperties(ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed) {
      TNTPrimed tnt = (TNTPrimed) event.getEntity();

      if (this.properties.fuse != null) {
        tnt.setFuseTicks(this.getFuseTicks());
      }

      if (this.properties.power != null) {
        tnt.setYield(this.properties.power); // Note: not related to EntityExplodeEvent.yield
      }
    }
  }

  // Make sure this event handler is called before the one in DispenserTracker that clears the
  // placer
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void dispenserNukes(BlockTransformEvent event) {
    BlockState oldState = event.getOldState();
    if (oldState instanceof Dispenser
        && this.properties.dispenserNukeLimit > 0
        && this.properties.dispenserNukeMultiplier > 0
        && event.getCause() instanceof EntityExplodeEvent) {

      EntityExplodeEvent explodeEvent = (EntityExplodeEvent) event.getCause();
      Dispenser dispenser = (Dispenser) oldState;
      int tntLimit =
          Math.round(this.properties.dispenserNukeLimit / this.properties.dispenserNukeMultiplier);
      int tntCount = 0;

      ItemStack[] inv = dispenser.getInventory().getContents();
      for (int slot = 0; slot < inv.length; slot++) {
        ItemStack stack = inv[slot];
        if (stack != null && stack.getType() == Material.TNT) {
          int transfer = Math.min(stack.getAmount(), tntLimit - tntCount);
          if (transfer > 0) {
            stack.setAmount(stack.getAmount() - transfer);
            tntCount += transfer;
            dispenser.getInventory().setItem(slot, stack);
          }
        }
      }

      tntCount = (int) Math.ceil(tntCount * this.properties.dispenserNukeMultiplier);

      for (int i = 0; i < tntCount; i++) {
        TNTPrimed tnt = match.getWorld().spawn(dispenser.getLocation(), TNTPrimed.class);

        tnt.setFuseTicks(10
            + match
                .getRandom()
                .nextInt(10)); // between 0.5 and 1.0 seconds, same as vanilla TNT chaining

        Random random = match.getRandom();
        Vector velocity = new Vector(
            random.nextGaussian(),
            random.nextGaussian(),
            random.nextGaussian()); // uniform random direction
        velocity.normalize().multiply(0.5 + 0.5 * random.nextDouble());
        tnt.setVelocity(velocity);

        callPrimeEvent(tnt, explodeEvent.getEntity());
      }
    }
  }
}
