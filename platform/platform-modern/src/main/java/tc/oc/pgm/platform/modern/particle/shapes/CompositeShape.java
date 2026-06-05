package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class CompositeShape implements ParticleShape {
  private final List<LineShape.Line> lines;
  private final List<CurveShape.Curve> curves;

  public CompositeShape(List<LineShape.Line> lines, List<CurveShape.Curve> curves) {
    this.lines = new ArrayList<>(lines);
    this.curves = new ArrayList<>(curves);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    double totalLength = 0;

    for (LineShape.Line line : lines) {
      totalLength += line.origin().distance(line.destination());
    }
    for (CurveShape.Curve curve : curves) {
      totalLength += ParticleShape.curveLength(curve, 16);
    }

    List<Vector> nodes = new ArrayList<>();

    for (LineShape.Line line : lines) {
      double ratio = line.origin().distance(line.destination()) / totalLength;
      int nodeCount = (int) (amount * ratio);
      ParticleShape.addEdgeNodes(nodes, new LineShape.Line[] {line}, nodeCount);
    }
    for (CurveShape.Curve curve : curves) {
      double ratio = ParticleShape.curveLength(curve, 16) / totalLength;
      int nodeCount = (int) (amount * ratio);
      ParticleShape.addCurveNodes(nodes, List.of(curve), nodeCount);
    }
    return nodes;
  }
}
