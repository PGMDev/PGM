package tc.oc.pgm.events;

import static net.kyori.adventure.text.Component.translatable;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;
import tc.oc.pgm.join.JoinRequest;

public abstract class PlayerParticipationEvent extends MatchPlayerEvent implements Cancellable {

  /** NOTE: this Competitor MAY not be in the match at this point */
  @Getter
  private final Competitor competitor;

  private final JoinRequest request;
  private boolean cancelled;
  private @Nullable Component cancelReason;

  protected PlayerParticipationEvent(
      MatchPlayer player, Competitor competitor, JoinRequest request) {
    super(player);
    this.competitor = competitor;
    this.request = request;
  }

  @NotNull
  public JoinRequest getRequest() {
    return request;
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
    return cancelReason != null ? cancelReason : translatable("error.unknown");
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
