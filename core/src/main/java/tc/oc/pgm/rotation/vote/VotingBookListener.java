package tc.oc.pgm.rotation.vote;

import static tc.oc.pgm.rotation.vote.MapPoll.VOTE_BOOK_METADATA;
import static tc.oc.pgm.rotation.vote.MapPoll.VOTE_BOOK_TAG;

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

  private final MapPoll poll;
  private final Match match;

  public VotingBookListener(MapPoll poll, Match match) {
    this.poll = poll;
    this.match = match;
  }

  @EventHandler
  public void openVote(PlayerInteractEvent event) {
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player != null
        && poll.isRunning()
        && isRightClick(event.getAction())
        && event.getMaterial() == Material.ENCHANTED_BOOK) {
      String validator = VOTE_BOOK_TAG.get(event.getItem());
      if (validator != null && validator.equals(VOTE_BOOK_METADATA)) poll.sendBook(player, true);
    }
  }

  private boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }
}
