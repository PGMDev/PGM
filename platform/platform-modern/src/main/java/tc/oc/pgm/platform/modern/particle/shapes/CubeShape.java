package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class CubeShape implements ParticleShape {
  private final float scale;
  private final float yaw;
  private final float pitch;

  public CubeShape(float scale, float yaw, float pitch) {
    this.scale = scale;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerLine = amount / 12;
    double half = scale / 2;
    List<Vector> nodes = new ArrayList<>();

    LineShape.Line[] lines = {
      new LineShape.Line(new Vector(-half, -half, -half), new Vector(half, -half, -half)),
      new LineShape.Line(new Vector(half, -half, -half), new Vector(half, -half, half)),
      new LineShape.Line(new Vector(half, -half, half), new Vector(-half, -half, half)),
      new LineShape.Line(new Vector(-half, -half, half), new Vector(-half, -half, -half)),
      new LineShape.Line(new Vector(-half, half, -half), new Vector(half, half, -half)),
      new LineShape.Line(new Vector(half, half, -half), new Vector(half, half, half)),
      new LineShape.Line(new Vector(half, half, half), new Vector(-half, half, half)),
      new LineShape.Line(new Vector(-half, half, half), new Vector(-half, half, -half)),
      new LineShape.Line(new Vector(-half, -half, -half), new Vector(-half, half, -half)),
      new LineShape.Line(new Vector(half, -half, -half), new Vector(half, half, -half)),
      new LineShape.Line(new Vector(half, -half, half), new Vector(half, half, half)),
      new LineShape.Line(new Vector(-half, -half, half), new Vector(-half, half, half)),
    };

    ParticleShape.addEdgeNodes(nodes, lines, nodesPerLine);

    ParticleShape.applyRotation(nodes, yaw, pitch);
    return nodes;
  }
}
