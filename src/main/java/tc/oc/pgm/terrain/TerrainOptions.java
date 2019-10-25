package tc.oc.pgm.terrain;

import java.io.File;
import javax.annotation.Nullable;

public class TerrainOptions {
  final File worldFolder;
  final boolean vanilla;
  final @Nullable Long seed;

  public TerrainOptions(File worldFolder, boolean vanilla, Long seed) {
    this.worldFolder = worldFolder;
    this.vanilla = vanilla;
    this.seed = seed;
  }
}
