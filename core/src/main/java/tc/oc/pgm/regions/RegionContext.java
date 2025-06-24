package tc.oc.pgm.regions;

import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.collection.ContextStore;

/**
 * Class that manages many named regions.
 *
 * <p>The RegionManager correlates regions with names so they can be looked up and resolved at a
 * later time.
 */
public class RegionContext extends ContextStore<Region> {}
