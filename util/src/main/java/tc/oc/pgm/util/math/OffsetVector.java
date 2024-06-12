package tc.oc.pgm.util.math;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public abstract class OffsetVector {
  protected final Vector vector;

  protected OffsetVector(Vector vector) {
    this.vector = vector;
  }

  public static OffsetVector of(Vector vector, boolean[] relative, boolean local) {
    if (local) return new Local(vector);

    boolean rel = relative[0];
    if (relative[1] == rel && relative[2] == rel) {
      return rel ? new Relative(vector) : new Absolute(vector);
    }
    return new Mixed(vector, relative);
  }

  public boolean isAbsolute() {
    return this instanceof Absolute;
  }

  public Vector getVector() {
    if (!isAbsolute())
      throw new UnsupportedOperationException("Can only get vector for absolute vectors");
    return vector;
  }

  public abstract Location applyOffset(Location origin);

  private static class Absolute extends OffsetVector {
    public Absolute(Vector vector) {
      super(vector);
    }

    public Location applyOffset(Location origin) {
      return copy(origin, vector.getX(), vector.getY(), vector.getZ());
    }
  }

  private static class Relative extends OffsetVector {
    public Relative(Vector vector) {
      super(vector);
    }

    public Location applyOffset(Location origin) {
      return origin.clone().add(vector);
    }
  }

  private static class Mixed extends OffsetVector {
    private final boolean[] relative;

    public Mixed(Vector vector, boolean[] relative) {
      super(vector);
      this.relative = relative;
    }

    public Location applyOffset(Location origin) {
      return copy(
          origin,
          (relative[0] ? origin.getX() : 0) + vector.getX(),
          (relative[1] ? origin.getY() : 0) + vector.getY(),
          (relative[2] ? origin.getZ() : 0) + vector.getZ());
    }
  }

  /**
   * Uses local coordinates (left, up, front), which are always relative. These coordinates are also
   * known as caret notation or ^ΔSway ^ΔHeave ^ΔSurge
   */
  private static class Local extends OffsetVector {
    public Local(Vector vector) {
      super(vector);
    }

    public Location applyOffset(Location origin) {
      Location newLoc = origin.clone();

      double pitch = Math.toRadians(origin.getPitch() - 90);
      double yaw = Math.toRadians(origin.getYaw() - 90);

      Vector sway = new Vector(-sin(yaw), 0, cos(yaw)).multiply(vector.getX());
      Vector heave = new Vector(0, -sin(pitch), cos(pitch)).multiply(vector.getY());
      Vector surge = origin.getDirection().normalize().multiply(vector.getZ());

      return newLoc.add(sway).add(heave).add(surge);
    }
  }

  private static Location copy(Location origin, double x, double y, double z) {
    return new Location(origin.getWorld(), x, y, z, origin.getYaw(), origin.getPitch());
  }
}
