package tc.oc.pgm.score;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerScoreEvent extends MatchEvent {

  private static final HandlerList handlers = new HandlerList();

  private final MatchPlayer player;
  private final double score;

  public PlayerScoreEvent(MatchPlayer player, double score) {
    super(player.getMatch());
    this.player = player;
    this.score = score;
  }

  public MatchPlayer getPlayer() {
    return player;
  }

  public double getScore() {
    return score;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
