package tc.oc.pgm.listeners;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.block.BlockVectorSet;
import tc.oc.pgm.util.bukkit.GameRules;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.material.Materials;

public class WorldProblemListener implements Listener {

  private static final int RANDOM_TICK_SPEED_LIMIT = 30;

  private final Logger logger;
  private final SetMultimap<World, Chunk> repairedChunks = HashMultimap.create();
  private static final Map<World, BlockVectorSet> block36Locations =
      new DefaultMapAdapter<>(world -> new BlockVectorSet(), true);

  public WorldProblemListener(Plugin plugin) {
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
  }

  void broadcastDeveloperWarning(String message) {
    logger.warning(message);
    Bukkit.broadcast(ChatColor.RED + message, Permissions.DEBUG);
  }

  @EventHandler
  public void warnRandomTickRate(WorldLoadEvent event) {
    var randomTickSpeed = GameRules.RANDOM_TICK_SPEED.get(event.getWorld());

    if (randomTickSpeed != null && randomTickSpeed > RANDOM_TICK_SPEED_LIMIT) {
      broadcastDeveloperWarning("Gamerule " + GameRules.RANDOM_TICK_SPEED.name() + " is set to "
          + randomTickSpeed
          + " for this world (normal value is 3). This may overload the server.");
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void unloadWorld(WorldUnloadEvent event) {
    this.repairedChunks.removeAll(event.getWorld());
    block36Locations.remove(event.getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void repairChunk(ChunkLoadEvent event) {
    if (this.repairedChunks.put(event.getWorld(), event.getChunk())) {
      // Set by modern platform on 1.13+ worlds - we want to treat only the older worlds
      if (!event.getWorld().hasMetadata("is-post-flattening")) {
        // Replace formerly invisible half-iron-door blocks with barriers
        for (Block ironDoor : NMS_HACKS.getBlocks(event.getChunk(), Materials.IRON_DOOR)) {
          BlockFace half =
              MATERIAL_UTILS.isUpperHalfOfDoor(ironDoor) ? BlockFace.UP : BlockFace.DOWN;
          if (ironDoor.getRelative(half.getOppositeFace()).getType() != Materials.IRON_DOOR) {
            ironDoor.setType(Material.BARRIER, false);
          }
        }
      }

      // Remove all block 36 and remember the ones at y=0 so VoidFilter can check them
      for (Block block36 : NMS_HACKS.getBlocks(event.getChunk(), Materials.MOVING_PISTON)) {
        if (block36.getY() == 0) {
          block36Locations
              .get(event.getWorld())
              .add(block36.getX(), block36.getY(), block36.getZ());
        }
        block36.setType(Material.AIR, false);
      }
    }
  }

  public static boolean wasBlock36(World world, int x, int y, int z) {
    return block36Locations.get(world).contains(x, y, z);
  }
}
