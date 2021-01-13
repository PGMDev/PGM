package tc.oc.pgm.destroyable;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

/** Abstract superclass for {@link Destroyable} related events. */
public abstract class DestroyableEvent extends MatchEvent {
  private final @NonNull Destroyable destroyable;

  public DestroyableEvent(@NonNull Match match, @NonNull Destroyable destroyable) {
    super(match);

    Preconditions.checkNotNull(destroyable, "destroyable");

    this.destroyable = destroyable;
  }

  /**
   * Gets the destroyable involved in this event.
   *
   * @return Destroyable involved
   */
  public @NonNull Destroyable getDestroyable() {
    return this.destroyable;
  }
}
