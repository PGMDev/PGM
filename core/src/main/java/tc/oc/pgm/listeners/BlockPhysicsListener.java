package tc.oc.pgm.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

@NullMarked
public class BlockPhysicsListener implements Listener {

  protected boolean allowPhysics(World world) {
    Match match = PGM.get().getMatchManager().getMatch(world);
    if (match == null) return !Match.isMatchWorld(world);
    if (match.isFinished()) return false;
    if (match.isRunning()) return true;
    return match.getMap().getWorld().initialPhysics();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockPhysics(BlockPhysicsEvent event) {
    if (!allowPhysics(event.getBlock().getWorld())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockFromTo(BlockFromToEvent event) {
    if (!allowPhysics(event.getBlock().getWorld())) event.setCancelled(true);
  }
}
