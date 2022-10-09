package tc.oc.pgm.destroyable;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

/** Abstract superclass for {@link Destroyable} related events. */
public abstract class DestroyableEvent extends MatchEvent {
  private final @NotNull Destroyable destroyable;

  public DestroyableEvent(@NotNull Match match, @NotNull Destroyable destroyable) {
    super(match);
    this.destroyable = assertNotNull(destroyable, "destroyable");
  }

  /**
   * Gets the destroyable involved in this event.
   *
   * @return Destroyable involved
   */
  public @NotNull Destroyable getDestroyable() {
    return this.destroyable;
  }
}
