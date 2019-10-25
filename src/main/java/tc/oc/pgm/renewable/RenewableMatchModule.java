package tc.oc.pgm.renewable;

import java.util.List;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchScope;

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
      getMatch().registerEvents(renewable, MatchScope.RUNNING);
      getMatch().registerTickable(renewable, MatchScope.RUNNING);
    }
  }
}
