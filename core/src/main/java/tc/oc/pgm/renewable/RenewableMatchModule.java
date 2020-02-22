package tc.oc.pgm.renewable;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.util.ClassLogger;

public class RenewableMatchModule implements MatchModule {

  private final Match match;
  private final List<RenewableDefinition> definitions;

  public RenewableMatchModule(Match match, List<RenewableDefinition> definitions) {
    this.match = match;
    this.definitions = definitions;
  }

  @Override
  public void load() {
    for (RenewableDefinition definition : definitions) {
      Renewable renewable =
          new Renewable(definition, match, ClassLogger.get(match.getLogger(), Renewable.class));
      match.addListener(renewable, MatchScope.RUNNING);
      match.addTickable(renewable, MatchScope.RUNNING);
    }
  }
}
