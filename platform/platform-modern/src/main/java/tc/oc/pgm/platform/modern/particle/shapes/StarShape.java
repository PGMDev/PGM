package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class StarShape extends AbstractShapePreset {
  public StarShape(float scale, float yaw, float pitch) {
    super(scale, yaw, pitch);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerLine = amount / 10;
    List<Vector> nodes = new ArrayList<>();

    double outerRadius = scale;
    double innerRadius = scale * 0.4;

    Vector[] points = new Vector[10];
    for (int i = 0; i < 10; i++) {
      double angle = Math.toRadians(i * 36 - 90);
      double r = (i % 2 == 0) ? outerRadius : innerRadius;
      points[i] = new Vector(r * Math.cos(angle), 0, r * Math.sin(angle));
    }

    LineShape.Line[] lines = new LineShape.Line[10];
    for (int i = 0; i < 10; i++) {
      lines[i] = new LineShape.Line(points[i], points[(i + 1) % 10]);
    }

    ParticleShape.addEdgeNodes(nodes, lines, nodesPerLine);
    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
