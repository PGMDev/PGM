package tc.oc.pgm.points;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.bukkit.WorldBorders;

public class RegionPointProvider implements PointProvider {

  private final Region region;
  private final PointProviderAttributes attributes;

  public RegionPointProvider(Region region, PointProviderAttributes attributes) {
    this.attributes = attributes;
    this.region = assertNotNull(region, "region");
    ;
  }

  @Override
  public Region getRegion() {
    return region;
  }

  @Override
  public boolean canFail() {
    return attributes.isSafe();
  }

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    Vector pos = this.region.getRandom(match);
    PointProviderLocation location =
        makeSafe(new PointProviderLocation(match.getWorld(), pos.getX(), pos.getY(), pos.getZ()));

    if (location == null) return null;

    if (attributes.getYawProvider() != null) {
      location.setYaw(attributes.getYawProvider().getAngle(pos));
      location.setHasYaw(true);
    }

    if (attributes.getPitchProvider() != null) {
      location.setPitch(attributes.getPitchProvider().getAngle(pos));
      location.setHasPitch(true);
    }

    return location;
  }

  private @Nullable PointProviderLocation makeSafe(@NotNull PointProviderLocation location) {
    // If the initial point is safe, just return it
    if (isSpawnable(location)) return location;

    // Try centering the point in its block
    location = location.clone();
    location.setX(location.getBlockX() + 0.5);
    location.setY(location.getBlockY());
    location.setZ(location.getBlockZ() + 0.5);
    if (isSpawnable(location)) return location;

    int scanDirection;
    if (attributes.isOutdoors()) {
      location.setY(Math.max(location.getY(), location.getWorld().getHighestBlockYAt(location)));
      scanDirection = 1;
    } else {
      scanDirection = -1;
    }

    // Scan downward, then upward, for a safe point in the region. If spawn is outdoors, just scan
    // upward.
    for (; scanDirection <= 1; scanDirection += 2) {
      for (PointProviderLocation safe = location.clone();
          safe.getBlockY() >= 0 && safe.getBlockY() < 256 && region.contains(safe);
          safe.setY(safe.getBlockY() + scanDirection)) {

        if (isSpawnable(safe)) return safe;
      }
    }

    // Give up
    return null;
  }

  private boolean isSpawnable(Location location) {
    return (!attributes.isSafe() || isSafe(location))
        && (!attributes.isOutdoors() || isOutdoors(location));
  }

  /**
   * Indicates whether or not this spawn is safe.
   *
   * @param location Location to check for.
   * @return True or false depending on whether this is a safe spawn point.
   */
  private boolean isSafe(Location location) {
    if (!WorldBorders.isInsideBorder(location)) return false;

    Block block = location.getBlock();
    Block above = block.getRelative(BlockFace.UP);
    Block below = block.getRelative(BlockFace.DOWN);

    return block.isEmpty() && above.isEmpty() && BlockVectors.isSupportive(below.getType());
  }

  private boolean isOutdoors(Location location) {
    return location.getWorld().getHighestBlockYAt(location) <= location.getBlockY();
  }
}
