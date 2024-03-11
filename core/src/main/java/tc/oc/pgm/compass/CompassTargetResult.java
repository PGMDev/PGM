package tc.oc.pgm.compass;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class CompassTargetResult {
  private final Location location;
  private final double distance;
  private final Component component;

  public CompassTargetResult(Location location, double distance, Component component) {
    this.location = location;
    this.distance = distance;
    this.component = component;
  }

  public Location getLocation() {
    return location;
  }

  public double getDistance() {
    return distance;
  }

  public Component getComponent() {
    return component;
  }

  public int compareTo(CompassTargetResult other) {
    return (int) (distance - other.distance);
  }
}
