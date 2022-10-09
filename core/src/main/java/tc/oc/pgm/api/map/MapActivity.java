package tc.oc.pgm.api.map;

import org.jetbrains.annotations.Nullable;

/* Represents data related to persisting map pool info between server restarts */
public interface MapActivity {

  /**
   * Get the name of the map pool.
   *
   * @return The name of the map pool
   */
  String getPoolName();

  /**
   * For rotation based pools, will be the map that is resumed upon server restart.
   *
   * @return The name of a map, or null
   */
  @Nullable
  String getMapName();

  /**
   * Whether this map pool was the last active. If the map pool was the last active, it will be
   * resumed upon server restart.
   *
   * @return Whether the map pool is active
   */
  boolean isActive();

  /**
   * Updates the map activity.
   *
   * @param nextMap Name of next map (if rotation based).
   * @param active If this pool is currently active.
   */
  void update(@Nullable String nextMap, boolean active);
}
