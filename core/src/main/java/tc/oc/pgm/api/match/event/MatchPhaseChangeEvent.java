package tc.oc.pgm.api.match.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;

/**
 * Called when a {@link Match} changes its {@link MatchPhase}.
 *
 * @see MatchStartEvent
 * @see MatchFinishEvent
 * @see Match#setPhase(MatchPhase)
 */
public class MatchPhaseChangeEvent extends MatchEvent {

  private final @Nullable MatchPhase oldState;
  private final MatchPhase newState;

  public MatchPhaseChangeEvent(Match match, @Nullable MatchPhase oldState, MatchPhase newState) {
    super(match);

    this.oldState = oldState;
    this.newState = assertNotNull(newState, "new match state");
  }

  /**
   * Get the previous {@link MatchPhase} of the {@link Match}.
   *
   * @return The previous {@link MatchPhase}.
   */
  public final @Nullable MatchPhase getOldPhase() {
    return oldState;
  }

  /**
   * Get the new, and current, {@link MatchPhase} of the {@link Match}.
   *
   * @return The new {@link MatchPhase}.
   */
  public final MatchPhase getNewPhase() {
    return newState;
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
