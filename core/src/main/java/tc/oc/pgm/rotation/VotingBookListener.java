package tc.oc.pgm.rotation;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
final class VotingBookListener implements Listener {

  private final VotingPool votingPool;
  private final Match match;

  public VotingBookListener(VotingPool votingPool, Match match) {
    this.votingPool = votingPool;
    this.match = match;
  }

  @EventHandler
  public void openVote(PlayerInteractEvent event) {
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (isRightClick(event.getAction())
        && event.getMaterial() == Material.ENCHANTED_BOOK
        && player != null) {
      votingPool.getCurrentPoll().sendBook(player, true);
    }
  }

  private boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }
}
