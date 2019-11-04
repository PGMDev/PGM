package tc.oc.pgm.api.match;

import tc.oc.pgm.api.time.Tick;

/** Represents a {@link Match} object that is called every {@link Tick}. */
public interface Tickable {

  /**
   * Fired every {@link Tick} during a {@link Match}.
   *
   * @see Match#addTickable(Tickable, MatchScope)
   * @param match The {@link Match}.
   * @param tick The current {@link Tick}.
   */
  void tick(Match match, Tick tick);
}
