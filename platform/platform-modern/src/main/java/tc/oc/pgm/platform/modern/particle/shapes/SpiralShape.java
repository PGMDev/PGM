package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class SpiralShape implements ParticleShape {
  public enum SpiralAxis {
    X,
    Y,
    Z
  }

  public record Spiral(int turns, float radius, SpiralAxis axis) {}

  private final List<Spiral> spirals;

  public SpiralShape(List<Spiral> spirals) {
    this.spirals = new ArrayList<>(spirals);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    List<Vector> nodes = new ArrayList<>();
    int nodesPerSpiral = amount / spirals.size();
    for (Spiral spiral : spirals) {
      for (int i = 0; i < nodesPerSpiral; i++) {
        double t = (double) i / nodesPerSpiral;
        double angle = t * spiral.turns() * 2 * Math.PI;
        double r = t * spiral.radius();
        double a = r * Math.cos(angle);
        double b = r * Math.sin(angle);
        nodes.add(
            switch (spiral.axis()) {
              case X -> new Vector(0, a, b);
              case Y -> new Vector(a, 0, b);
              case Z -> new Vector(a, b, 0);
            });
      }
    }
    return nodes;
  }
}
