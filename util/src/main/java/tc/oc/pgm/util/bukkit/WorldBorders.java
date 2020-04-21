package tc.oc.pgm.util.bukkit;

import org.bukkit.Location;
import org.bukkit.WorldBorder;

public interface WorldBorders {

  static boolean isInsideBorder(Location location) {
    WorldBorder border = location.getWorld().getWorldBorder();
    Location center = border.getCenter();
    double radius = border.getSize() / 2d;
    return Math.abs(location.getX() - center.getX()) < radius
        && Math.abs(location.getZ() - center.getZ()) < radius;
  }

  static boolean clampToBorder(Location location) {
    WorldBorder border = location.getWorld().getWorldBorder();
    Location center = border.getCenter();
    double radius = border.getSize() / 2d;
    double xMin = center.getX() - radius;
    double xMax = center.getX() + radius;
    double zMin = center.getZ() - radius;
    double zMax = center.getZ() + radius;

    boolean moved = false;

    if (location.getX() < xMin) {
      location.setX(xMin);
      moved = true;
    }

    if (location.getX() > xMax) {
      location.setX(xMax);
      moved = true;
    }

    if (location.getZ() < zMin) {
      location.setZ(zMin);
      moved = true;
    }

    if (location.getZ() > zMax) {
      location.setZ(zMax);
      moved = true;
    }

    return moved;
  }
}
