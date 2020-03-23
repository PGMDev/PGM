package tc.oc.pgm.controlpoint;

import com.google.common.collect.Sets;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.util.MatchPlayers;

/** Tracks which players are on a control point and answers some queries about them */
public class ControlPointPlayerTracker implements Listener {
  protected final Match match;
  protected final Region captureRegion;
  protected final Set<MatchPlayer> playersOnPoint = Sets.newHashSet();

  public ControlPointPlayerTracker(Match match, Region captureRegion) {
    this.match = match;
    this.captureRegion = captureRegion;
  }

  public Set<MatchPlayer> getPlayersOnPoint() {
    return this.playersOnPoint;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final CoarsePlayerMoveEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  private void handlePlayerMove(Player bukkit, Vector to) {
    MatchPlayer player = this.match.getPlayer(bukkit);
    if (!MatchPlayers.canInteract(player)) return;

    if (!player.getBukkit().isDead() && this.captureRegion.contains(to.toBlockVector())) {
      this.playersOnPoint.add(player);
    } else {
      this.playersOnPoint.remove(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    playersOnPoint.remove(event.getPlayer());
  }
}
