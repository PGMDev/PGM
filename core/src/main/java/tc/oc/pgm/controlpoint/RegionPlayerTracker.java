package tc.oc.pgm.controlpoint;

import com.google.common.collect.Sets;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/** Tracks which players are on a control point and answers some queries about them */
public class RegionPlayerTracker implements Listener {
  private final Match match;
  private final Set<MatchPlayer> players = Sets.newHashSet();

  // The region to check against
  private Region.Static region;
  // A static filter players must match when entering
  private final @Nullable Filter staticFilter;

  public RegionPlayerTracker(Match match, Region region) {
    this(match, region, null);
  }

  public RegionPlayerTracker(Match match, Region region, @Nullable Filter staticFilter) {
    this.match = match;
    this.region = region.getStatic(match);
    this.staticFilter = staticFilter;
  }

  public Set<MatchPlayer> getPlayers() {
    return this.players;
  }

  public void setRegion(Region.Static region) {
    this.region = region;
    for (MatchPlayer player : match.getPlayers()) {
      handlePlayerMove(player.getBukkit(), player.getLocation().toVector());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final PlayerCoarseMoveEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  public void handlePlayerMove(Player bukkit, Vector to) {
    MatchPlayer player = this.match.getPlayer(bukkit);
    if (!MatchPlayers.canInteract(player)) return;

    if (!player.getBukkit().isDead()
        && this.region.contains(to.toBlockVector())
        && (this.staticFilter == null || this.staticFilter.query(player).isAllowed())) {
      this.players.add(player);
    } else {
      this.players.remove(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    players.remove(event.getPlayer());
  }
}
