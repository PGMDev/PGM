package tc.oc.pgm.points;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.Region;
import tc.oc.util.bukkit.WorldBorders;
import tc.oc.util.bukkit.block.BlockVectors;

public class RegionPointProvider implements PointProvider {

  private final Region region;
  private final PointProviderAttributes attributes;

  public RegionPointProvider(Region region, PointProviderAttributes attributes) {
    this.attributes = attributes;
    this.region = checkNotNull(region, "region");
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
    Vector pos = this.region.getRandom(match.getRandom());
    PointProviderLocation location =
        new PointProviderLocation(match.getWorld(), pos.getX(), pos.getY(), pos.getZ());

    if (attributes.getYawProvider() != null) {
      location.setYaw(attributes.getYawProvider().getAngle(pos));
      location.setHasYaw(true);
    }

    if (attributes.getPitchProvider() != null) {
      location.setPitch(attributes.getPitchProvider().getAngle(pos));
      location.setHasPitch(true);
    }

    location = makeSafe(location);

    return location;
  }

  private PointProviderLocation makeSafe(PointProviderLocation location) {
    if (location == null) return null;

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
    if (attributes.isSafe() && !isSafe(location)) return false;
    if (attributes.isOutdoors() && !isOutdoors(location)) return false;
    return true;
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
