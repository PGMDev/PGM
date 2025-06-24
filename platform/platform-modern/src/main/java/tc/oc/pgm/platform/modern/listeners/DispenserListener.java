package tc.oc.pgm.platform.modern.listeners;

import static tc.oc.pgm.util.event.EventUtil.handleCall;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import tc.oc.pgm.util.event.block.BlockDispenseEntityEvent;

public class DispenserListener implements Listener {

  private BlockDispenseEvent lastEvent;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDispense(BlockDispenseEvent event) {
    Block block = event.getBlock();
    if (!(block.getBlockData() instanceof Dispenser dispenser)) return;
    var placeLocation = event.getVelocity();
    var frontBlock = block.getRelative(dispenser.getFacing()).getLocation().toVector();

    // When placing/removing a block (eg: water bucket), velocity is the block location
    // When spawning entities, it's either:
    //  - zero (0,0,0) for item or egg spawning
    //  - the direction (eg: 0,0,-1) for projectiles
    //  - the entity location (eg: 12.5,10.0,1.5) for tnt or boats
    // If the location is exactly the block, ignore it as it's not an entity dispense
    if (placeLocation.equals(frontBlock) && !placeLocation.isZero()) return;
    lastEvent = event;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onEntitySpawn(EntitySpawnEvent event) {
    if (lastEvent != null && !event.isCancelled())
      handleEntitySpawn(lastEvent, event, event.getEntity());
    lastEvent = null;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onVehicleSpawn(VehicleCreateEvent event) {
    // Dispensing boats/minecarts/TNT minecarst creates no EntitySpawnEvent
    if (lastEvent != null && !event.isCancelled())
      handleEntitySpawn(lastEvent, event, event.getVehicle());
    lastEvent = null;
  }

  public void handleEntitySpawn(BlockDispenseEvent dispenseEvent, Event spawnEvent, Entity entity) {
    Block block = dispenseEvent.getBlock();
    if (!(block.getBlockData() instanceof Dispenser dispenser)) return;
    Block expected = block.getRelative(dispenser.getFacing());
    Location actual = entity.getLocation();

    double xDiff = Math.abs(actual.getX() - ((double) expected.getX() + 0.5d));
    double yDiff = actual.getY() - expected.getY();
    double zDiff = Math.abs(actual.getZ() - ((double) expected.getZ() + 0.5d));

    // Within same block x/z, and up to 1.5 blocks up in Y dir (spawning a boat in water)
    if (xDiff > 0.5 || yDiff < 0 || yDiff > 1.5 || zDiff > 0.5) return;

    BlockDispenseEntityEvent pgmEvent = new BlockDispenseEntityEvent(
        dispenseEvent.getBlock(), dispenseEvent.getItem(), dispenseEvent.getVelocity(), entity);
    handleCall(pgmEvent, spawnEvent);
  }
}
