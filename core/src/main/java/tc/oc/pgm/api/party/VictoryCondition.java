package tc.oc.pgm.api.party;

import java.util.Comparator;
import net.kyori.text.Component;
import tc.oc.pgm.api.match.Match;

/**
 * A condition used to decide if a {@link tc.oc.pgm.api.party.Competitor} has won the {@link Match},
 * and for measuring how close competitors are to winning, relative to each other.
 *
 * <p>If multiple competitors compare equal, they are tied.
 */
public interface VictoryCondition extends Comparator<Competitor> {
  /**
   * It's lame that all the subclasses need to be listed here, but I can't come up with a better way
   * to ensure they are evaluated in the right order that isn't overly complex or prone to bugs.
   */
  enum Priority {
    IMMEDIATE,
    TIME_LIMIT,
    BLITZ,
    SCORE,
    GOALS
  }

  Priority getPriority();

  /**
   * Test if this victory condition has been satisfied for the given match. When this returns true,
   * the match will finish, and the lowest {@link tc.oc.pgm.api.party.Competitor}s in the ordering
   * will be the winners.
   */
  boolean isCompleted(Match match);

  /**
   * If true, the ordering of this condition is the final result, and ties cannot be resolved by any
   * other conditions.
   */
  boolean isFinal(Match match);

  /** Return -1 if a is ahead of b, 1 if b is ahead of a, and 0 if a and b are tied */
  int compare(Competitor a, Competitor b);

  /**
   * Return a short (one line) description of this type of result.
   *
   * @return
   */
  Component getDescription(Match match);
}
