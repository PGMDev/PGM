package tc.oc.pgm.spawner;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.renewable.Renewable;
import tc.oc.pgm.renewable.RenewableDefinition;
import tc.oc.pgm.util.ClassLogger;

import java.util.List;

public class SpawnerMatchModule implements MatchModule {

    private Match match;
    private List<SpawnerDefinition> definitions;

    public SpawnerMatchModule(Match match, List<SpawnerDefinition> definitions) {
        this.match = match;
        this.definitions = definitions;
    }

    @Override
    public void load() {
        for (SpawnerDefinition definition : definitions) {
            Spawner spawner =
                    new Spawner(definition, match, ClassLogger.get(match.getLogger(), Spawner.class));
            match.addListener(spawner, MatchScope.RUNNING);
            match.addTickable(spawner, MatchScope.RUNNING);
        }
    }
}
