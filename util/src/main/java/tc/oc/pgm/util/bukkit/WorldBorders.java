package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.util.NumberConversions;

public class WorldBorders {

  private WorldBorders() {}

  public static boolean isInsideBorder(Location location) {
    BorderRect rect = new BorderRect(location.getWorld());
    return location.getBlockX() >= rect.xMin
        && location.getBlockX() <= rect.xMax
        && location.getBlockZ() >= rect.zMin
        && location.getBlockZ() <= rect.zMax;
  }

  public static boolean clampToBorder(Location location) {
    BorderRect rect = new BorderRect(location.getWorld());

    boolean moved = false;

    if (location.getX() <= rect.xMin) {
      location.setX(rect.xMin + 0.5);
      moved = true;
    } else if (location.getX() >= rect.xMax) {
      location.setX(rect.xMax - 0.5);
      moved = true;
    }

    if (location.getZ() <= rect.zMin) {
      location.setZ(rect.zMin + 0.5);
      moved = true;
    } else if (location.getZ() >= rect.zMax) {
      location.setZ(rect.zMax - 0.5);
      moved = true;
    }

    return moved;
  }

  private static class BorderRect {
    public final int xMin, xMax, zMin, zMax;

    public BorderRect(World world) {
      WorldBorder border = world.getWorldBorder();
      Location center = border.getCenter();
      double radius = border.getSize() / 2d;
      int maxWorldSize = NMS_HACKS.getMaxWorldSize(world);

      xMin = Math.max(NumberConversions.floor(center.getX() - radius), -maxWorldSize);
      xMax = Math.min(NumberConversions.ceil(center.getX() + radius), maxWorldSize);
      zMin = Math.max(NumberConversions.floor(center.getZ() - radius), -maxWorldSize);
      zMax = Math.min(NumberConversions.ceil(center.getZ() + radius), maxWorldSize);
    }
  }
}
