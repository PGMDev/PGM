package tc.oc.pgm.destroyable;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

/**
 * Called when an a {@link Destroyable} is damaged or repaired.
 *
 * <p>Event is fired before the info is added to the destroyable's list.
 *
 * @see DestroyableHealthChange
 */
public class DestroyableHealthChangeEvent extends DestroyableEvent {
  public DestroyableHealthChangeEvent(
      @NotNull Match match,
      @NotNull Destroyable destroyable,
      @Nullable DestroyableHealthChange change) {
    super(match, destroyable);
    this.change = change;
  }

  /**
   * Gets the information associated with this event. This may be null in cases where there are no
   * details available about the event. In this case, anything about the Destroyable could have
   * changed.
   *
   * @return Event information
   */
  public @Nullable DestroyableHealthChange getChange() {
    return this.change;
  }

  private final @Nullable DestroyableHealthChange change;

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
