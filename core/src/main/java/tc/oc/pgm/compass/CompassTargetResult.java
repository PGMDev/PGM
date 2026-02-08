package tc.oc.pgm.compass;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

@Getter
@AllArgsConstructor
public class CompassTargetResult {
  private final Location location;
  private final double distance;
  private final double yDifference;
  private final Component component;

  public static CompassTargetResult of(Location target, Location holder, Component component) {
    double yDifference = target.getY() - holder.getY();
    return new CompassTargetResult(target, holder.distance(target), yDifference, component);
  }

  public int compareTo(CompassTargetResult other) {
    return Double.compare(this.distance, other.distance);
  }
}
