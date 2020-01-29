package tc.oc.pgm.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.block.BlockVectorSet;
import tc.oc.pgm.api.Permissions;
import tc.oc.util.ClassLogger;
import tc.oc.util.collection.DefaultMapAdapter;
import tc.oc.world.NMSHacks;

public class WorldProblemListener implements Listener {

  private static final int RANDOM_TICK_SPEED_LIMIT = 30;

  private final Logger logger;
  private final SetMultimap<World, Chunk> repairedChunks = HashMultimap.create();
  private static final Map<World, BlockVectorSet> block36Locations =
      new DefaultMapAdapter<>(new BlockVectorSet.Factory<>(), true);

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
  public void warnUncachedSkulls(ChunkLoadEvent event) {
    for (BlockState state : event.getChunk().getTileEntities()) {
      if (state instanceof Skull) {
        if (!NMSHacks.isSkullCached((Skull) state)) {
          Location loc = state.getLocation();
          broadcastDeveloperWarning(
              "Uncached skull \""
                  + ((Skull) state).getOwner()
                  + "\" at "
                  + loc.getBlockX()
                  + ", "
                  + loc.getBlockY()
                  + ", "
                  + loc.getBlockZ());
        }
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
      // Replace formerly invisible half-iron-door blocks with barriers
      for (Block ironDoor : event.getChunk().getBlocks(Material.IRON_DOOR_BLOCK)) {
        BlockFace half = (ironDoor.getData() & 8) == 0 ? BlockFace.DOWN : BlockFace.UP;
        if (ironDoor.getRelative(half.getOppositeFace()).getType() != Material.IRON_DOOR_BLOCK) {
          ironDoor.setType(Material.BARRIER, false);
        }
      }

      // Remove all block 36 and remember the ones at y=0 so VoidFilter can check them
      for (Block block36 : event.getChunk().getBlocks(Material.PISTON_MOVING_PIECE)) {
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void fixVillagerTrades(ChunkLoadEvent event) {
    for (Entity entity : event.getChunk().getEntities()) {
      if (entity instanceof Villager) {
        NMSHacks.fixVillagerTrades((Villager) entity);
      }
    }
  }
}
