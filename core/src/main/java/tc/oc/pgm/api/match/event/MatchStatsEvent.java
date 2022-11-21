package tc.oc.pgm.api.match.event;

import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.stats.PlayerStats;

public class MatchStatsEvent extends MatchEvent implements Cancellable {

  private boolean cancelled = false;
  private boolean showBest;
  private boolean showOwn;

  private Map<UUID, PlayerStats> stats;

  public MatchStatsEvent(
      Match match, boolean showBest, boolean showOwn, Map<UUID, PlayerStats> stats) {
    super(match);
    this.showBest = showBest;
    this.showOwn = showOwn;
    this.stats = stats;
  }

  public boolean isShowBest() {
    return showBest;
  }

  public void setShowBest(boolean showBest) {
    this.showBest = showBest;
  }

  public boolean isShowOwn() {
    return showOwn;
  }

  public void setShowOwn(boolean showOwn) {
    this.showOwn = showOwn;
  }

  public Map<UUID, PlayerStats> getStats() {
    return stats;
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
