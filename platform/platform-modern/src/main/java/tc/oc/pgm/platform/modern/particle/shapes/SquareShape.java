package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class SquareShape extends AbstractShapePreset {
  public SquareShape(float scale, float yaw, float pitch) {
    super(scale, yaw, pitch);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerLine = amount / 4;
    double half = scale / 2;
    List<Vector> nodes = new ArrayList<>();

    LineShape.Line[] lines = {
      new LineShape.Line(new Vector(half, 0, half), new Vector(-half, 0, half)),
      new LineShape.Line(new Vector(-half, 0, half), new Vector(-half, 0, -half)),
      new LineShape.Line(new Vector(-half, 0, -half), new Vector(half, 0, -half)),
      new LineShape.Line(new Vector(half, 0, -half), new Vector(half, 0, half))
    };

    ParticleShape.addEdgeNodes(nodes, lines, nodesPerLine);

    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
