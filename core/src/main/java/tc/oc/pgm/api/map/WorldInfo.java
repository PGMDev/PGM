package tc.oc.pgm.api.map;

import org.bukkit.World;

/** Customized settings when creating a {@link org.bukkit.World}. */
public interface WorldInfo {

  /**
   * Get the random "seed" for generating terrain.
   *
   * <p>Will only have an affect when {@link #hasTerrain()} is {@code true}.
   *
   * @return A random seed.
   */
  long getSeed();

  /**
   * Get whether there should be "vanilla" terrain.
   *
   * <p>Otherwise, new {@link org.bukkit.Chunk}s that load will be empty.
   *
   * @return If vanilla terrain should be generated.
   */
  boolean hasTerrain();

  /**
   * Get the type of {@link World.Environment#ordinal()}.
   *
   * @return The world environment type.
   */
  World.Environment getEnvironment();
}
