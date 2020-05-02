package tc.oc.pgm.spawner;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;

public class SpawnerMatchModule implements MatchModule {

  private Match match;
  private final List<SpawnerDefinition> definitions;

  public SpawnerMatchModule(Match match, List<SpawnerDefinition> definitions) {
    this.match = match;
    this.definitions = definitions;
  }

  @Override
  public void load() {
    for (SpawnerDefinition definition : definitions) {
      Spawner spawner = new Spawner(definition, match);
      match.addListener(spawner, MatchScope.RUNNING);
      match.addTickable(spawner, MatchScope.RUNNING);
    }
  }
}
