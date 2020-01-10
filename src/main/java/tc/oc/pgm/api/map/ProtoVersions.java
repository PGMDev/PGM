package tc.oc.pgm.api.map;

import tc.oc.util.SemanticVersion;

public class ProtoVersions {
  // Version that fixed the off-by-one region bug
  public static final SemanticVersion REGION_FIX_VERSION = new SemanticVersion(1, 3, 1);

  // Version that introduced monument modes
  public static final SemanticVersion MODES_IMPLEMENTATION_VERSION = new SemanticVersion(1, 3, 2);

  // First proto to define the way overlapping regions behave
  public static final SemanticVersion REGION_PRIORITY_VERSION = new SemanticVersion(1, 3, 3);

  // Wool locations required
  public static final SemanticVersion WOOL_LOCATIONS = new SemanticVersion(1, 3, 4);

  // Filters know who owns TNT
  public static final SemanticVersion FILTER_OWNED_TNT = new SemanticVersion(1, 3, 5);

  // Move all defining elements out of module xml root
  public static final SemanticVersion MODULE_SUBELEMENT_VERSION = new SemanticVersion(1, 3, 6);

  // Everything scores zero points by default
  public static final SemanticVersion DEFAULT_SCORES_TO_ZERO = new SemanticVersion(1, 3, 6);

  // Filters/regions/teams always referenced by ID
  public static final SemanticVersion FILTER_FEATURES = new SemanticVersion(1, 4, 0);

  // Disallow <time> inside <score> or <blitz>
  public static final SemanticVersion REMOVE_SCORE_TIME_LIMIT = new SemanticVersion(1, 4, 0);

  // Disallow <title> inside <blitz>
  public static final SemanticVersion REMOVE_BLITZ_TITLE = new SemanticVersion(1, 4, 0);

  // Option on objectives to determine if they are required to win the match
  public static final SemanticVersion GOAL_REQUIRED_OPTION = new SemanticVersion(1, 4, 0);
}
