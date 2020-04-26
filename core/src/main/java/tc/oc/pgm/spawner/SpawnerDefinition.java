package tc.oc.pgm.spawner;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

import java.time.Duration;

public class SpawnerDefinition {

    public Region region;
    public String id;
    public int count;
    public int playerRange;
    public int maxEntities;
    public Duration minDelay, maxDelay, delay;
    public Filter filter;

}
