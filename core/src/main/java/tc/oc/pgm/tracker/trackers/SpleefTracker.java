package tc.oc.pgm.tracker.trackers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.api.event.PlayerSpleefEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.tracker.info.*;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ExplosionInfo;
import tc.oc.pgm.tracker.info.PlayerInfo;
import tc.oc.pgm.tracker.info.SpleefInfo;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.material.Materials;

/**
 * Tracks blocks broken by players and fires a {@link PlayerSpleefEvent} when it appears to cause a
 * player to leave the ground.
 */
public class SpleefTracker implements Listener {

  private static final float PLAYER_WIDTH = 0.6f;
  private static final float PLAYER_RADIUS = PLAYER_WIDTH / 2.0f;

  // A player must leave the ground within this many ticks of a block being broken
  // under them for the fall to be caused by a spleef from that block
  public static final long MAX_SPLEEF_TICKS = 20;

  private final TrackerMatchModule tracker;
  private final Match match;
  private final Map<Block, SpleefInfo> brokenBlocks = new HashMap<>();

  public SpleefTracker(TrackerMatchModule tracker) {
    this.tracker = tracker;
    this.match = tracker.getMatch();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(final ParticipantBlockTransformEvent event) {
    if (!event.isBreak()) return;
    if (!Materials.isSolid(event.getOldState())) return;
    if (!match.isRunning()) return;

    final Block block = event.getBlock();
    DamageInfo breaker = null;

    if (event.getCause() instanceof EntityExplodeEvent) {
      PhysicalInfo explosive =
          tracker.resolveInfo(
              ((EntityExplodeEvent) event.getCause()).getEntity(), PhysicalInfo.class);
      if (explosive != null) {
        breaker = new ExplosionInfo(explosive);
      }
    }

    if (breaker == null) {
      breaker = new PlayerInfo(event.getPlayerState());
    }

    final SpleefInfo info = new SpleefInfo(breaker, match.getTick());
    brokenBlocks.put(block, info);

    match
        .getExecutor(MatchScope.RUNNING)
        .schedule(
            () -> {
              // Only remove the BrokenBlock if it's the same one we added. It may have been
              // replaced since then.
              if (info == brokenBlocks.get(block)) {
                brokenBlocks.remove(block);
              }
            },
            (MAX_SPLEEF_TICKS + 1) * TimeUtils.TICK,
            TimeUnit.MILLISECONDS);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (player == null) return;

    Block block = this.lastBlockBrokenUnderPlayer(player);
    if (block != null) {
      SpleefInfo info = brokenBlocks.get(block);
      if (match.getTick().tick - info.getTime().tick <= MAX_SPLEEF_TICKS) {
        match.callEvent(new PlayerSpleefEvent(player, block, info));
      }
    }
  }

  public Block lastBlockBrokenUnderPlayer(MatchPlayer player) {
    Location playerLocation = player.getBukkit().getLocation();

    int y = (int) Math.floor(playerLocation.getY() - 0.1);

    int x1 = (int) Math.floor(playerLocation.getX() - PLAYER_RADIUS);
    int z1 = (int) Math.floor(playerLocation.getZ() - PLAYER_RADIUS);

    int x2 = (int) Math.floor(playerLocation.getX() + PLAYER_RADIUS);
    int z2 = (int) Math.floor(playerLocation.getZ() + PLAYER_RADIUS);

    long latestTick = Long.MIN_VALUE;
    Block latestBlock = null;

    for (int x = x1; x <= x2; ++x) {
      for (int z = z1; z <= z2; ++z) {
        Block block = playerLocation.getBlock();
        SpleefInfo info = this.brokenBlocks.get(block);
        if (info != null) {
          long tick = info.getTime().tick;
          if (tick > latestTick) {
            latestTick = tick;
            latestBlock = block;
          }
        }
      }
    }

    return latestBlock;
  }
}
