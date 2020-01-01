package tc.oc.pgm.terrain;

import java.io.File;
import javax.annotation.Nullable;

public class TerrainOptions {
  public final File worldFolder;
  public final boolean vanilla;
  public final @Nullable Long seed;

  public TerrainOptions(File worldFolder, boolean vanilla, Long seed) {
    this.worldFolder = worldFolder;
    this.vanilla = vanilla;
    this.seed = seed;
  }
}
