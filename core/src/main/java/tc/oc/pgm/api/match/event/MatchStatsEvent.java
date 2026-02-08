package tc.oc.pgm.api.match.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

public class MatchStatsEvent extends MatchEvent implements Cancellable {

  private boolean cancelled = false;

  @Getter
  @Setter
  private boolean showBest;

  @Getter
  @Setter
  private boolean showOwn;

  public MatchStatsEvent(Match match, boolean showBest, boolean showOwn) {
    super(match);
    this.showBest = showBest;
    this.showOwn = showOwn;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
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
