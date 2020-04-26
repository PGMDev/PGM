package tc.oc.pgm.spawner;

import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;

import java.util.logging.Logger;

public class Spawner implements Listener, Tickable {

    private final SpawnerDefinition definition;
    private final Match match;
    private final Logger logger;

    private long lastTick;

    public Spawner(SpawnerDefinition definition, Match match, Logger logger) {
        this.definition = definition;
        this.match = match;
        this.logger = logger;

        this.lastTick = match.getTick().tick;
    }

    @Override
    public void tick(Match match, Tick tick) {

    }
}
