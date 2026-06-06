package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class CircleShape extends AbstractShapePreset {
  public CircleShape(float scale, float yaw, float pitch) {
    super(scale, yaw, pitch);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    double angleInc = (double) 360 / amount;
    List<Vector> nodes = new ArrayList<>();
    for (int i = 0; i < amount; i++) {
      double x = Math.cos(Math.toRadians(i * angleInc)) * scale;
      double z = Math.sin(Math.toRadians(i * angleInc)) * scale;
      nodes.add(new Vector(x, 0, z));
    }

    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
