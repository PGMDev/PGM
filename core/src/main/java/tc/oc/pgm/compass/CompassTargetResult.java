package tc.oc.pgm.compass;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class CompassTargetResult {
  private final Location location;
  private final double distance;
  private final double yDifference;
  private final Component component;

  public CompassTargetResult(
      Location location, double distance, double yDifference, Component component) {
    this.location = location;
    this.distance = distance;
    this.yDifference = yDifference;
    this.component = component;
  }

  public static CompassTargetResult of(Location target, Location holder, Component component) {
    double yDifference = target.getY() - holder.getY();
    return new CompassTargetResult(target, holder.distance(target), yDifference, component);
  }

  public Location getLocation() {
    return location;
  }

  public double getDistance() {
    return distance;
  }

  public double getYDifference() {
    return yDifference;
  }

  public Component getComponent() {
    return component;
  }

  public int compareTo(CompassTargetResult other) {
    return Double.compare(this.distance, other.distance);
  }
}
