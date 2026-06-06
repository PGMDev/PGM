package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class SphereShape extends AbstractShapePreset {
  public SphereShape(float scale, float yaw, float pitch) {
    super(scale, yaw, pitch);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    List<Vector> nodes = new ArrayList<>();
    double phi = (1 + Math.sqrt(5)) / 2;
    for (int i = 0; i < amount; i++) {
      double angle = 2 * Math.PI * i * (1 - 1 / phi);
      double y = 1 - (2.0 * i) / (amount - 1);
      double r = Math.sqrt(1 - Math.pow(y, 2));
      double x = r * Math.cos(angle);
      double z = r * Math.sin(angle);
      nodes.add(new Vector(x * scale, y * scale, z * scale));
    }

    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
