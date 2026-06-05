package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class TriangleShape implements ParticleShape {
  private final float scale;
  private final float yaw;
  private final float pitch;

  public TriangleShape(float scale, float yaw, float pitch) {
    this.scale = scale;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerLine = amount / 3;
    List<Vector> nodes = new ArrayList<>();

    double ang1 = Math.toRadians(0);
    double ang2 = Math.toRadians(120);
    double ang3 = Math.toRadians(240);

    Vector c1 = new Vector(Math.cos(ang1) * scale, 0, Math.sin(ang1) * scale);
    Vector c2 = new Vector(Math.cos(ang2) * scale, 0, Math.sin(ang2) * scale);
    Vector c3 = new Vector(Math.cos(ang3) * scale, 0, Math.sin(ang3) * scale);

    LineShape.Line[] lines = {
      new LineShape.Line(c1, c2), new LineShape.Line(c2, c3), new LineShape.Line(c3, c1)
    };

    ParticleShape.addEdgeNodes(nodes, lines, nodesPerLine);

    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
