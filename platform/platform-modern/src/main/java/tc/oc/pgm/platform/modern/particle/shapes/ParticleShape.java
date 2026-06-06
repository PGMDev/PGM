package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.List;
import org.bukkit.util.Vector;

public interface ParticleShape {
  List<Vector> getNodes(int amount);

  static void addEdgeNodes(List<Vector> nodes, LineShape.Line[] lines, int nodesPerLine) {
    for (LineShape.Line line : lines) {
      Vector direction = line.destination().clone().subtract(line.origin());
      for (int i = 0; i < nodesPerLine; i++) {
        double progress = (double) i / nodesPerLine;
        nodes.add(line.origin().clone().add(direction.clone().multiply(progress)));
      }
    }
  }

  static void addCurveNodes(List<Vector> nodes, List<CurveShape.Curve> curves, int nodesPerCurve) {
    for (CurveShape.Curve curve : curves) {
      for (int i = 0; i < nodesPerCurve; i++) {
        double t = (double) i / nodesPerCurve;
        double x;
        double y;
        double z;
        if (curve.controlB() == null) {
          x = Math.pow(1 - t, 2) * curve.origin().getX()
              + 2 * (1 - t) * t * curve.controlA().getX()
              + Math.pow(t, 2) * curve.destination().getX();
          y = Math.pow(1 - t, 2) * curve.origin().getY()
              + 2 * (1 - t) * t * curve.controlA().getY()
              + Math.pow(t, 2) * curve.destination().getY();
          z = Math.pow(1 - t, 2) * curve.origin().getZ()
              + 2 * (1 - t) * t * curve.controlA().getZ()
              + Math.pow(t, 2) * curve.destination().getZ();
        } else {
          x = Math.pow(1 - t, 3) * curve.origin().getX()
              + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getX()
              + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getX()
              + Math.pow(t, 3) * curve.destination().getX();
          y = Math.pow(1 - t, 3) * curve.origin().getY()
              + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getY()
              + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getY()
              + Math.pow(t, 3) * curve.destination().getY();
          z = Math.pow(1 - t, 3) * curve.origin().getZ()
              + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getZ()
              + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getZ()
              + Math.pow(t, 3) * curve.destination().getZ();
        }
        nodes.add(new Vector(x, y, z));
      }
    }
  }

  static double curveLength(CurveShape.Curve curve, int samples) {
    double length = 0;
    Vector prev = sampleCurve(curve, 0);
    for (int i = 1; i <= samples; i++) {
      double t = (double) i / samples;
      Vector current = sampleCurve(curve, t);
      length += prev.distance(current);
      prev = current;
    }
    return length;
  }

  static Vector sampleCurve(CurveShape.Curve curve, double t) {
    double x;
    double y;
    double z;
    if (curve.controlB() == null) {
      x = Math.pow(1 - t, 2) * curve.origin().getX()
          + 2 * (1 - t) * t * curve.controlA().getX()
          + Math.pow(t, 2) * curve.destination().getX();
      y = Math.pow(1 - t, 2) * curve.origin().getY()
          + 2 * (1 - t) * t * curve.controlA().getY()
          + Math.pow(t, 2) * curve.destination().getY();
      z = Math.pow(1 - t, 2) * curve.origin().getZ()
          + 2 * (1 - t) * t * curve.controlA().getZ()
          + Math.pow(t, 2) * curve.destination().getZ();
    } else {
      x = Math.pow(1 - t, 3) * curve.origin().getX()
          + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getX()
          + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getX()
          + Math.pow(t, 3) * curve.destination().getX();
      y = Math.pow(1 - t, 3) * curve.origin().getY()
          + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getY()
          + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getY()
          + Math.pow(t, 3) * curve.destination().getY();
      z = Math.pow(1 - t, 3) * curve.origin().getZ()
          + 3 * Math.pow(1 - t, 2) * t * curve.controlA().getZ()
          + 3 * (1 - t) * Math.pow(t, 2) * curve.controlB().getZ()
          + Math.pow(t, 3) * curve.destination().getZ();
    }
    return new Vector(x, y, z);
  }

  static void applyRotation(List<Vector> nodes, float yaw, float pitch) {
    if (pitch != 0 || yaw != 0) {
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);

      for (Vector node : nodes) {
        double x = node.getX();
        double y = node.getY();
        double z = node.getZ();

        double newX = x * Math.cos(yawRad) - z * Math.sin(yawRad);
        double newZ = x * Math.sin(yawRad) + z * Math.cos(yawRad);

        double newY = y * Math.cos(pitchRad) - newZ * Math.sin(pitchRad);
        double finalZ = y * Math.sin(pitchRad) + newZ * Math.cos(pitchRad);

        node.setX(newX);
        node.setY(newY);
        node.setZ(finalZ);
      }
    }
  }
}
