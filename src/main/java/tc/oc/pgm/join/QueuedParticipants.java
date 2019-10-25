package tc.oc.pgm.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.bukkit.ChatColor;
import tc.oc.pgm.Config;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.ObservingParty;

/**
 * Observing party that holds players who have requested to join before match start, when the server
 * is configured to defer joins. After the match starts, this party is empty.
 */
public class QueuedParticipants extends ObservingParty {

  /** Order that queued joins are processed */
  static class JoinOrder implements Comparator<MatchPlayer> {
    @Override
    public int compare(MatchPlayer a, MatchPlayer b) {
      return Boolean.compare(b.canPriorityKick(), a.canPriorityKick());
    }
  }

  private List<MatchPlayer> shuffledPlayers;

  public QueuedParticipants(Match match) {
    super(match);
  }

  private void invalidateShuffle() {
    shuffledPlayers = null;
  }

  @Override
  public boolean addPlayer(MatchPlayer player) {
    if (super.addPlayer(player)) {
      invalidateShuffle();
      return true;
    }
    return false;
  }

  @Override
  public boolean removePlayer(MatchPlayer player) {
    if (super.removePlayer(player)) {
      invalidateShuffle();
      return true;
    }
    return false;
  }

  public List<MatchPlayer> getOrderedPlayers() {
    if (shuffledPlayers == null) {
      shuffledPlayers = new ArrayList<>(getPlayers());
      Collections.shuffle(shuffledPlayers);

      if (Config.Join.priorityKick()) {
        // If priority kicking is enabled, might as well join the high
        // priority players first so nobody actually gets kicked.
        Collections.sort(shuffledPlayers, new JoinOrder());
      }
    }
    return shuffledPlayers;
  }

  @Override
  public String getDefaultName() {
    return "Participants";
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.YELLOW;
  }
}
