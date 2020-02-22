package tc.oc.pgm.result;

import tc.oc.pgm.api.match.Match;

/** Base class for victory conditions that finish the match immediately and unambiguously. */
public abstract class ImmediateVictoryCondition implements VictoryCondition {

  @Override
  public Priority getPriority() {
    return Priority.IMMEDIATE;
  }

  @Override
  public boolean isCompleted(Match match) {
    return true;
  }

  @Override
  public boolean isFinal(Match match) {
    return true;
  }
}
