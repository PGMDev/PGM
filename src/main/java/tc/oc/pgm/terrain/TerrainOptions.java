package tc.oc.pgm.terrain;

import javax.annotation.Nullable;

public class TerrainOptions {
  final boolean vanilla;
  final @Nullable Long seed;

  public TerrainOptions(boolean vanilla, Long seed) {
    this.vanilla = vanilla;
    this.seed = seed;
  }
}
