package tc.oc.pgm.api.map;

import tc.oc.util.Version;

/** Various {@link MapInfo#getProto()} versions that introduce breaking syntax changes. */
public interface MapProtos {

  // Version that fixed the off-by-one region bug
  Version REGION_FIX_VERSION = new Version(1, 3, 1);

  // Version that introduced monument modes
  Version MODES_IMPLEMENTATION_VERSION = new Version(1, 3, 2);

  // First proto to define the way overlapping regions behave
  Version REGION_PRIORITY_VERSION = new Version(1, 3, 3);

  // Wool locations required
  Version WOOL_LOCATIONS = new Version(1, 3, 4);

  // Filters know who owns TNT
  Version FILTER_OWNED_TNT = new Version(1, 3, 5);

  // Move all defining elements out of module xml root
  Version MODULE_SUBELEMENT_VERSION = new Version(1, 3, 6);

  // Everything scores zero points by default
  Version DEFAULT_SCORES_TO_ZERO = new Version(1, 3, 6);

  // Filters/regions/teams always referenced by ID
  Version FILTER_FEATURES = new Version(1, 4, 0);

  // Disallow <time> inside <score> or <blitz>
  Version REMOVE_SCORE_TIME_LIMIT = new Version(1, 4, 0);

  // Disallow <title> inside <blitz>
  Version REMOVE_BLITZ_TITLE = new Version(1, 4, 0);

  // Option on objectives to determine if they are required to win the match
  Version GOAL_REQUIRED_OPTION = new Version(1, 4, 0);
}
