package tc.oc.pgm.regions;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/** Keeps track of players in the given {@link Region} */
public class RegionPlayerTracker implements Listener {
  private final Match match;
  private Region region;
  private final Set<MatchPlayer> players = new HashSet<>();

  public RegionPlayerTracker(Match match, Region region) {
    this.match = match;
    this.region = region;
    match.addListener(this, MatchScope.RUNNING);
  }

  public Set<MatchPlayer> getPlayersInRegion() {
    return players;
  }

  public void setRegion(Region region) {
    this.region = region;
    updateNearbyPlayersManual();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final PlayerCoarseMoveEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    this.handlePlayerMove(event.getPlayer(), event.getTo().toVector());
  }

  // Used when moving regions to prevent players standing still staying in the region forever
  public void updateNearbyPlayersManual() {
    match
        .getPlayers()
        .forEach(p -> handlePlayerMove(p.getBukkit(), p.getBukkit().getLocation().toVector()));
  }

  private void handlePlayerMove(Player bukkit, Vector to) {
    MatchPlayer player = this.match.getPlayer(bukkit);
    if (!MatchPlayers.canInteract(player)) return;

    if (!player.isDead() && region.contains(to.toBlockVector())) {
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
