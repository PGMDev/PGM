package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class PlaneShape extends AbstractShapePreset {
  public PlaneShape(float scale, float yaw, float pitch) {
    super(scale, yaw, pitch);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int side = (int) Math.sqrt(amount);
    double step = scale / side;
    double half = scale / 2;
    List<Vector> nodes = new ArrayList<>();

    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        double x = -half + i * step;
        double z = -half + j * step;
        nodes.add(new Vector(x, 0, z));
      }
    }
    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
