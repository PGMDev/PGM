package tc.oc.pgm.controlpoint;

import com.google.common.collect.Sets;
import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.util.collection.DefaultMapAdapter;

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

  /** Get the number of players that each team in the match has on the point */
  public Map<Team, Integer> getPlayerCountsByTeam() {
    // calculate how many players from each team are on the hill
    Map<Team, Integer> counts = new DefaultMapAdapter<>(new HashMap<Team, Integer>(), 0);
    for (MatchPlayer player : this.getPlayersOnPoint()) {
      Team team = Teams.get(player);
      counts.put(team, counts.get(team) + 1);
    }
    return counts;
  }

  /**
   * Get the number of players that each team in the match has on the point, sorted from most to
   * least
   */
  public List<Map.Entry<Team, Integer>> getSortedPlayerCountsByTeam() {
    List<Map.Entry<Team, Integer>> counts =
        new ArrayList<>(this.getPlayerCountsByTeam().entrySet());
    Collections.sort(
        counts,
        new Comparator<Map.Entry<Team, Integer>>() {
          @Override
          public int compare(Map.Entry<Team, Integer> o1, Map.Entry<Team, Integer> o2) {
            return Integer.compare(
                o2.getValue(), o1.getValue()); // reverse natural ordering of value
          }
        });
    return counts;
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
