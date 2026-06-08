package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public record CurveShape(List<Curve> curves) implements ParticleShape {
  public record Curve(
      Vector origin,
      Vector controlA,
      Vector destination,
      @Nullable Vector controlB) {
  }

  public CurveShape(List<Curve> curves) {
    this.curves = new ArrayList<>(curves);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    int nodesPerCurve = amount / curves.size();
    List<Vector> nodes = new ArrayList<>();

    ParticleShape.addCurveNodes(nodes, curves, nodesPerCurve);
    return nodes;
  }
}
