package tc.oc.pgm.blockdrops;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

public record BlockDropsRule(
    Filter filter,
    Region region,
    boolean dropOnWrongTool,
    boolean punch,
    boolean trample,
    BlockDrops drops) {}
