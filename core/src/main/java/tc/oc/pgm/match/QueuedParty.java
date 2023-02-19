package tc.oc.pgm.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;

/** A party that queues player joins before a match. */
public class QueuedParty extends PartyImpl {

  private class Order implements Comparator<MatchPlayer> {
    private final JoinMatchModule join = getMatch().needModule(JoinMatchModule.class);

    @Override
    public int compare(final MatchPlayer a, final MatchPlayer b) {
      return Boolean.compare(!join.canBePriorityKicked(b), !join.canBePriorityKicked(a));
    }
  }

  private List<MatchPlayer> memberOrder;

  public QueuedParty(final Match match) {
    super(match, "Participants", ChatColor.YELLOW, null);
  }

  @Override
  public boolean isObserving() {
    return true;
  }

  @Override
  public boolean isParticipating() {
    return false;
  }

  @Override
  public void addPlayer(final MatchPlayer player) {
    super.addPlayer(player);
    invalidateOrder();
  }

  @Override
  public void removePlayer(final UUID playerId) {
    super.removePlayer(playerId);
    invalidateOrder();
  }

  private void invalidateOrder() {
    this.memberOrder = null;
  }

  public List<MatchPlayer> getOrderedPlayers() {
    if (this.memberOrder == null) {
      this.memberOrder = new ArrayList<>(getPlayers());
      Collections.shuffle(this.memberOrder);

      if (PGM.get().getConfiguration().canPriorityKick()) {
        // If priority kicking is enabled, might as well join the high
        // priority players first so nobody actually gets kicked.
        Collections.sort(this.memberOrder, new Order());
      }
    }
    return this.memberOrder;
  }
}
