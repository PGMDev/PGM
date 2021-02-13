package tc.oc.pgm.stats;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

public class StatsDisplayEvent extends MatchEvent implements Cancellable {

  private boolean cancelled = false;
  private boolean showHigh;
  private boolean showOwn;

  public StatsDisplayEvent(Match match, boolean showHigh, boolean showOwn) {
    super(match);
    this.showHigh = showHigh;
    this.showOwn = showOwn;
  }

  public boolean isShowHigh() {
    return showHigh;
  }

  public void setShowHigh(boolean showHigh) {
    this.showHigh = showHigh;
  }

  public boolean isShowOwn() {
    return showOwn;
  }

  public void setShowOwn(boolean showOwn) {
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
