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
import org.bukkit.util.Vector;
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
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.material.Materials;

/**
 * Tracks blocks broken by players and fires a {@link PlayerSpleefEvent} when it appears to cause a
 * player to leave the ground.
 */
public class SpleefTracker implements Listener {

  private static final float PLAYER_WIDTH = 0.6f;
  private static final float PLAYER_RADIUS = PLAYER_WIDTH / 2.0f;
  private static final float BLOCK_OFFSET = PLAYER_RADIUS + 0.05f;

  // A player must leave the ground within this many ticks of a block being broken
  // under them for the fall to be caused by a spleef from that block
  public static final long MAX_SPLEEF_TICKS = 30;

  private final TrackerMatchModule tracker;
  private final Match match;
  private final Map<Vector, SpleefInfo> brokenBlocks = new HashMap<>();

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
    Vector pos = block.getLocation().toVector();
    brokenBlocks.put(pos, info);

    // Only remove the BrokenBlock if it's the same one we added. It may have been replaced since
    // then
    match
        .getExecutor(MatchScope.RUNNING)
        .schedule(
            () -> brokenBlocks.remove(pos, info),
            (MAX_SPLEEF_TICKS + 1) * TimeUtils.TICK,
            TimeUnit.MILLISECONDS);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    if (event.getOnGround()) return;
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (player == null) return;

    Vector pos = this.lastBlockBrokenUnderPlayer(player);
    if (pos != null) {
      SpleefInfo info = brokenBlocks.get(pos);
      if (match.getTick().tick - info.getTime().tick <= MAX_SPLEEF_TICKS) {
        match.callEvent(new PlayerSpleefEvent(player, pos, info));
      }
    }
  }

  public Vector lastBlockBrokenUnderPlayer(MatchPlayer player) {
    Location playerLocation = player.getBukkit().getLocation();

    int y = (int) Math.floor(playerLocation.getY() - 0.1);

    int x1 = (int) Math.floor(playerLocation.getX() - BLOCK_OFFSET);
    int z1 = (int) Math.floor(playerLocation.getZ() - BLOCK_OFFSET);

    int x2 = (int) Math.floor(playerLocation.getX() + BLOCK_OFFSET);
    int z2 = (int) Math.floor(playerLocation.getZ() + BLOCK_OFFSET);

    long latestTick = Long.MIN_VALUE;
    Vector latestPos = null;

    for (int x = x1; x <= x2; ++x) {
      for (int z = z1; z <= z2; ++z) {
        Vector pos = new Vector(x, y, z);
        SpleefInfo info = this.brokenBlocks.get(pos);
        if (info != null) {
          long tick = info.getTime().tick;
          if (tick > latestTick) {
            latestTick = tick;
            latestPos = pos;
          }
        }
      }
    }

    return latestPos;
  }
}
