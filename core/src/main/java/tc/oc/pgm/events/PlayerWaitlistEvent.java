package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;
import tc.oc.pgm.join.JoinRequest;

/** Called after a player is added to the waitlist of a full match. */
public class PlayerWaitlistEvent extends MatchPlayerEvent {

  private final JoinRequest joinRequest;
  private final int waitlistSize;

  public PlayerWaitlistEvent(MatchPlayer player, JoinRequest request, int waitlistSize) {
    super(player);
    this.joinRequest = request;
    this.waitlistSize = waitlistSize;
  }

  public JoinRequest getJoinRequest() {
    return joinRequest;
  }

  /** The size of the waitlist after the player was added. */
  public int getWaitlistSize() {
    return waitlistSize;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
