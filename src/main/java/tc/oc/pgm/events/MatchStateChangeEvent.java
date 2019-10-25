package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchState;

public class MatchStateChangeEvent extends MatchEvent {
  public MatchStateChangeEvent(Match match, @Nullable MatchState oldState, MatchState newState) {
    super(match);

    this.oldState = oldState;
    this.newState = checkNotNull(newState, "new match state");
  }

  public @Nullable MatchState getOldState() {
    return this.oldState;
  }

  public MatchState getNewState() {
    return this.newState;
  }

  final @Nullable MatchState oldState;
  final MatchState newState;

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
