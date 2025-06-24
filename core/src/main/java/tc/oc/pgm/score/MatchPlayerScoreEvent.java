package tc.oc.pgm.score;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class MatchPlayerScoreEvent extends MatchPlayerEvent {

  private static final HandlerList handlers = new HandlerList();

  private final double score;
  private final ScoreCause cause;

  public MatchPlayerScoreEvent(MatchPlayer player, double score, ScoreCause cause) {
    super(player);
    this.score = score;
    this.cause = cause;
  }

  public double getScore() {
    return score;
  }

  public ScoreCause getCause() {
    return cause;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
