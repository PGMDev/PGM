package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class SpiralShape implements ParticleShape {
  public record Spiral(int turns, float radius) {}

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
        double x = r * Math.cos(angle);
        double z = r * Math.sin(angle);
        nodes.add(new Vector(x, 0, z));
      }
    }
    return nodes;
  }
}
