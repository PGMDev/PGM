package tc.oc.pgm.platform.modern;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.event.EventUtil;
import tc.oc.pgm.util.event.player.PlayerSpawnEntityEvent;
import tc.oc.pgm.util.material.MaterialMatcher;

public class SpawnEggUseListener implements Listener {

  private static final MaterialMatcher MATERIALS =
      MaterialMatcher.builder().addAll(m -> m.name().endsWith("_SPAWN_EGG")).build();

  private Player lastPlacer;
  private ItemStack placingStack;
  private Location placeLocation;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInteraction(PlayerInteractEvent event) {
    ItemStack stack;
    Block block;
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK
        && event.useItemInHand() != Event.Result.DENY
        && (stack = event.getItem()) != null
        && MATERIALS.matches(stack.getType())
        && (block = event.getClickedBlock()) != null) {
      lastPlacer = event.getPlayer();
      placingStack = stack.clone();
      // Vanilla behavior, no collision box (eg: torch) spawns in same block
      if (block.getCollisionShape().getBoundingBoxes().isEmpty()) {
        placeLocation = block.getLocation();
      } else {
        placeLocation = block.getRelative(event.getBlockFace()).getLocation();
      }
    } else {
      reset();
    }
  }

  /** Pre event has not modified spawn position, making it more accurate on weird placements* */
  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (lastPlacer != null && !event.isCancelled() && event.getSpawnReason() == SPAWNER_EGG) {
      Entity entity = event.getEntity();

      if (isSpawnLocFor(placeLocation, entity.getLocation())) {
        EventUtil.handleCall(new PlayerSpawnEntityEvent(lastPlacer, entity, placingStack), event);
      }
    }
    // Always reset after, cancelled or not
    reset();
  }

  private void reset() {
    lastPlacer = null;
    placingStack = null;
    placeLocation = null;
  }

  private static boolean isSpawnLocFor(Location initial, Location entity) {
    // Leniency in y-axis, as vanilla may move the entity 1 block up
    return entity.getBlockX() == initial.getBlockX()
        && entity.getBlockY() >= initial.getBlockY()
        && entity.getBlockY() <= initial.getBlockY() + 1
        && entity.getBlockZ() == initial.getBlockZ();
  }
}
