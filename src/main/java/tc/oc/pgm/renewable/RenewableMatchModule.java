package tc.oc.pgm.renewable;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.match.MatchModule;

public class RenewableMatchModule extends MatchModule {

  private final List<RenewableDefinition> definitions;

  public RenewableMatchModule(Match match, List<RenewableDefinition> definitions) {
    super(match);
    this.definitions = definitions;
  }

  @Override
  public void load() {
    super.load();

    for (RenewableDefinition definition : definitions) {
      Renewable renewable = new Renewable(definition, match, logger);
      getMatch().addListener(renewable, MatchScope.RUNNING);
      getMatch().addTickable(renewable, MatchScope.RUNNING);
    }
  }
}
