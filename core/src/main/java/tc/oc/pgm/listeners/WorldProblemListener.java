package tc.oc.pgm.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import tc.oc.pgm.util.collection.DefaultMapAdapter;

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
    String str = event.getWorld().getGameRuleValue("randomTickSpeed");
    if (str != null) {
      int value = Integer.parseInt(str);
      if (value > RANDOM_TICK_SPEED_LIMIT) {
        broadcastDeveloperWarning(
            "Gamerule 'randomTickSpeed' is set to "
                + value
                + " for this world (normal value is 3). This may overload the server.");
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void unloadWorld(WorldUnloadEvent event) {
    this.repairedChunks.removeAll(event.getWorld());
    block36Locations.remove(event.getWorld());
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void repairChunk(ChunkLoadEvent event) {
    if (this.repairedChunks.put(event.getWorld(), event.getChunk())) {
      // Remove all block 36 at y = 0 and remember them so VoidFilter can check them
      ChunkSnapshot chunkSnapshot = event.getChunk().getChunkSnapshot();

      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          Material blockType = chunkSnapshot.getBlockType(x, 0, z);
          if (blockType == Material.MOVING_PISTON) {
            Block block = event.getChunk().getBlock(x, 0, z);
            block.setType(Material.AIR);
            block36Locations.get(event.getWorld()).add(block.getX(), block.getY(), block.getZ());
          }
        }
      }
    }
  }

  public static boolean wasBlock36(World world, int x, int y, int z) {
    return block36Locations.get(world).contains(x, y, z);
  }
}
