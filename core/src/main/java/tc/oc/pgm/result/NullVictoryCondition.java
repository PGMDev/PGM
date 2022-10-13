package tc.oc.pgm.result;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;

/** Replacement for null value of victory condition */
public class NullVictoryCondition implements VictoryCondition {

  public static final NullVictoryCondition INSTANCE = new NullVictoryCondition();

  private NullVictoryCondition() {}

  @Override
  public Priority getPriority() {
    return null;
  }

  @Override
  public boolean isCompleted(Match match) {
    return false;
  }

  @Override
  public boolean isFinal(Match match) {
    return false;
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    return 0;
  }

  @Override
  public Component getDescription(Match match) {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    return false;
  }
}
