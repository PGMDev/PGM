package tc.oc.pgm.events;

import javax.annotation.Nullable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.MatchPlayer;

public abstract class PlayerParticipationEvent extends MatchPlayerEvent implements Cancellable {

  private final Competitor competitor;
  private boolean cancelled;
  private @Nullable Component cancelReason;

  protected PlayerParticipationEvent(MatchPlayer player, Competitor competitor) {
    super(player);
    this.competitor = competitor;
  }

  /** NOTE: this Competitor MAY not be in the match at this point */
  public Competitor getCompetitor() {
    return competitor;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean b) {
    this.cancelled = b;
  }

  public void cancel(Component reason) {
    setCancelled(true);
    this.cancelReason = reason;
  }

  public Component getCancelReason() {
    return cancelReason != null
        ? cancelReason
        : new PersonalizedText("Internal error changing player participation state");
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
