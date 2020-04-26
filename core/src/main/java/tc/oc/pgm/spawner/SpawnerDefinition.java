package tc.oc.pgm.spawner;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

import java.time.Duration;
import java.util.List;

public class SpawnerDefinition {

    public Region spawnRegion;
    public String id;
    public int count;
    public Region playerRegion;
    public int maxEntities;
    public Duration minDelay, maxDelay, delay;
    public Filter filter;
    public List<SpawnerObject> objects;

}
