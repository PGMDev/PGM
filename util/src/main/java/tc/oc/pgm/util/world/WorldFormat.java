package tc.oc.pgm.util.world;

import java.nio.file.Files;
import java.nio.file.Path;

/** The layout of a map's world folder. */
public enum WorldFormat {
  /** A legacy, CraftBukkit world folder, with a {@code level.dat} at its root. */
  LEGACY,
  /**
   * A Paper per-dimension folder, with world state split into {@code data/paper} and
   * {@code data/minecraft} saved-data files. Can be copied straight into the server's dimension
   * root.
   */
  DIMENSION;

  public static final String LEVEL_DAT = "level.dat";
  public static final String WORLD_GEN_SETTINGS = "data/minecraft/world_gen_settings.dat";

  public static WorldFormat detect(Path worldDir) {
    if (Files.isRegularFile(worldDir.resolve(LEVEL_DAT))) return LEGACY;
    if (Files.isRegularFile(worldDir.resolve(WORLD_GEN_SETTINGS))) return DIMENSION;
    return LEGACY;
  }
}
