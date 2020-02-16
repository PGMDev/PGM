package tc.oc.pgm.modules;

import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import tc.oc.block.BlockVectors;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerBlockTransformEvent;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.Team;

@ListenerScope(MatchScope.RUNNING)
public class LaneMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<Team, Region> lanes;
  private final Set<UUID> voidPlayers = Sets.newHashSet();

  public LaneMatchModule(Match match, Map<Team, Region> lanes) {
    this.match = match;
    this.lanes = lanes;
  }

  @Override
  public void disable() {
    this.voidPlayers.clear();
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void checkLaneMovement(final CoarsePlayerMoveEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null
        || !player.canInteract()
        || !(player.getParty() instanceof Team)
        || player.getBukkit().getGameMode() == GameMode.CREATIVE
        || event.getTo().getY() <= 0) return;

    Region laneRegion = this.lanes.get(player.getParty());
    if (laneRegion == null) return;

    boolean containsFrom = laneRegion.contains(event.getBlockFrom().toVector());
    boolean containsTo = laneRegion.contains(event.getBlockTo().toVector());

    // prevent ender pearling to the other lane
    if (!containsTo && event.getCause() instanceof PlayerTeleportEvent) {
      if (((PlayerTeleportEvent) event.getCause()).getCause() == TeleportCause.ENDER_PEARL) {
        event.setCancelled(true, new PersonalizedTranslatable("match.lane.enderPearl.disabled"));
        return;
      }
    }

    if (this.voidPlayers.contains(player.getId())) {
      event.getPlayer().setFallDistance(0);
      // they have been marked as "out of lane"
      if (containsTo && !containsFrom) {
        // prevent the player from re-entering the lane
        event.setCancelled(true, new PersonalizedTranslatable("match.lane.reEntry.disabled"));
      } else {
        // if they are going to land on something, teleport them underneith it
        Block under = event.getTo().clone().add(new Vector(0, -1, 0)).getBlock();
        if (under != null && under.getType() != Material.AIR) {
          // teleport them to the lowest block
          Vector safe = getSafeLocationUnder(under);
          Location l = event.getPlayer().getLocation();
          event.setTo(safe.toLocation(l.getWorld(), l.getYaw(), l.getPitch()));
        }
      }
    } else {
      if (!containsFrom && !containsTo) {
        // they are outside of the lane
        if (isIllegallyOutsideLane(laneRegion, event.getTo())) {
          this.voidPlayers.add(player.getId());
          event
              .getPlayer()
              .sendMessage(
                  ChatColor.RED
                      + AllTranslations.get().translate("match.lane.exit", player.getBukkit()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void preventBlockPlaceByVoidPlayer(final BlockTransformEvent event) {
    if (event instanceof PlayerBlockTransformEvent) {
      event.setCancelled(
          this.voidPlayers.contains(((PlayerBlockTransformEvent) event).getPlayerState().getId()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearLaneStatus(final ParticipantDespawnEvent event) {
    this.voidPlayers.remove(event.getPlayer().getId());
  }

  private static BlockFace[] CARDINAL_DIRECTIONS = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };
  private static BlockFace[] DIAGONAL_DIRECTIONS = {
    BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.NORTH_WEST
  };

  private static Block getAdjacentRegionBlock(Region region, Block origin) {
    for (BlockFace face :
        ObjectArrays.concat(CARDINAL_DIRECTIONS, DIAGONAL_DIRECTIONS, BlockFace.class)) {
      Block adjacent = origin.getRelative(face);
      if (region.contains(BlockVectors.center(adjacent).toVector())) {
        return adjacent;
      }
    }
    return null;
  }

  private static boolean isIllegalBlock(Region region, Block block) {
    Block adjacent = getAdjacentRegionBlock(region, block);
    return adjacent == null || BlockVectors.isSupportive(adjacent.getType());
  }

  private static boolean isIllegallyOutsideLane(Region lane, Location loc) {
    Block feet = loc.getBlock();
    if (feet == null) return false;

    if (isIllegalBlock(lane, feet)) {
      return true;
    }

    Block head = feet.getRelative(BlockFace.UP);
    if (head == null) return false;

    if (isIllegalBlock(lane, head)) {
      return true;
    }

    return false;
  }

  private static Vector getSafeLocationUnder(Block block) {
    World world = block.getWorld();
    for (int y = block.getY() - 2; y >= 0; y--) {
      Block feet = world.getBlockAt(block.getX(), y, block.getZ());
      Block head = world.getBlockAt(block.getX(), y + 1, block.getZ());
      if (feet.getType() == Material.AIR && head.getType() == Material.AIR) {
        return new Vector(block.getX() + 0.5, y, block.getZ() + 0.5);
      }
    }
    return new Vector(block.getX() + 0.5, -2, block.getZ() + 0.5);
  }
}
