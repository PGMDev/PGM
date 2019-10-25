package tc.oc.pgm.flag.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.state.State;
import tc.oc.pgm.goals.events.GoalEvent;

/** Fired AFTER any transition of the {@link State} of a {@link Flag} */
public class FlagStateChangeEvent extends GoalEvent {

  protected final Flag flag;
  protected final State oldState, newState;

  public FlagStateChangeEvent(Flag flag, State oldState, State newState) {
    super(flag, newState.getController());
    this.flag = flag;
    this.oldState = oldState;
    this.newState = newState;
  }

  public Flag getFlag() {
    return flag;
  }

  public State getOldState() {
    return oldState;
  }

  public State getNewState() {
    return newState;
  }

  // HandlerList crap

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
