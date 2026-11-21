package tc.oc.pgm.spawner;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class SpawnerMatchModule implements MatchModule {

  private final Match match;
  private final List<Spawner> spawners;

  public SpawnerMatchModule(Match match, List<Spawner> spawners) {
    this.match = match;
    this.spawners = spawners;
  }

  @Override
  public void load() {
    for (Spawner spawner : spawners) {
      spawner.registerEvents();
    }
  }

  @Override
  public void unload() {
    for (Spawner spawner : this.spawners) {
      spawner.unregisterEvents();
    }
  }
}
