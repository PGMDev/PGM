package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public record LineShape(List<Line> lines) implements ParticleShape {
  public record Line(Vector origin, Vector destination) {}

  public LineShape(List<Line> lines) {
    this.lines = new ArrayList<>(lines);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerLine = amount / lines.size();
    List<Vector> nodes = new ArrayList<>();
    for (Line line : lines) {
      Vector direction = line.destination().clone().subtract(line.origin());
      for (int i = 0; i < nodesPerLine; i++) {
        double progress = (double) i / nodesPerLine;
        nodes.add(line.origin().clone().add(direction.clone().multiply(progress)));
      }
    }
    return nodes;
  }
}
