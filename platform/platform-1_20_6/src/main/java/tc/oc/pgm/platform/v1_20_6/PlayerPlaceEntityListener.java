package tc.oc.pgm.platform.v1_20_6;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.event.EventUtil;
import tc.oc.pgm.util.event.player.PlayerSpawnEntityEvent;
import tc.oc.pgm.util.material.MaterialMatcher;

public class PlayerPlaceEntityListener implements Listener {

  private final MaterialMatcher material;

  private Player lastPlacer;
  private ItemStack placingStack;
  private Location placeLocation;

  public PlayerPlaceEntityListener(MaterialMatcher material) {
    this.material = material;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInteraction(PlayerInteractEvent event) {
    ItemStack stack = event.getItem();
    if (stack != null
        && material.matches(stack.getType())
        && event.useItemInHand() != Event.Result.DENY) {
      lastPlacer = event.getPlayer();
      placingStack = stack.clone();
      placeLocation = event.getInteractionPoint();
    }
  }

  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (lastPlacer != null && !event.isCancelled()) {
      Entity entity = event.getEntity();

      if (isSameBlock(placeLocation, entity.getLocation())) {
        var pgmEvent = new PlayerSpawnEntityEvent(lastPlacer, entity, placingStack);
        EventUtil.handleCall(pgmEvent, event);
      }
    }
    // Always reset after, cancelled or not
    lastPlacer = null;
    placingStack = null;
    placeLocation = null;
  }

  private static boolean isSameBlock(Location locA, Location locB) {
    return locA.getBlockX() == locB.getBlockX()
        && locA.getBlockY() == locB.getBlockY()
        && locA.getBlockZ() == locB.getBlockZ();
  }
}
