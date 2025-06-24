package tc.oc.pgm.util.math;

import org.bukkit.util.Vector;

public class TransformMatrix {

  private static final TransformMatrix IDENTITY = new TransformMatrix(new double[] {
    1, 0, 0, 0, //
    0, 1, 0, 0, //
    0, 0, 1, 0, //
    0, 0, 0, 1 //
  });
  private final double[] matrix;

  private TransformMatrix(double[] matrix) {
    this.matrix = matrix;
  }

  public static TransformMatrix identity() {
    return IDENTITY;
  }

  public static TransformMatrix translate(Vector vector) {
    return new TransformMatrix(new double[] {
      1, 0, 0, vector.getX(),
      0, 1, 0, vector.getY(),
      0, 0, 1, vector.getZ(),
      0, 0, 0, 1
    });
  }

  public static TransformMatrix untranslate(Vector vector) {
    return new TransformMatrix(new double[] {
      1, 0, 0, -vector.getX(),
      0, 1, 0, -vector.getY(),
      0, 0, 1, -vector.getZ(),
      0, 0, 0, 1
    });
  }

  public static TransformMatrix scale(Vector vector) {
    double x = vector.getX();
    double y = vector.getY();
    double z = vector.getZ();
    return new TransformMatrix(new double[] {
      x, 0, 0, 0, //
      0, y, 0, 0, //
      0, 0, z, 0, //
      0, 0, 0, 1 //
    });
  }

  public static TransformMatrix concat(TransformMatrix... a) {
    if (a.length == 1) return a[0];

    TransformMatrix result = a[a.length - 1];
    for (int i = a.length - 2; i >= 0; i--) {
      result = result.multiply(a[i]);
    }
    return result;
  }

  public TransformMatrix multiply(TransformMatrix other) {
    double[] a = this.matrix;
    double[] b = other.matrix;

    return new TransformMatrix(new double[] {
      a[0] * b[0] + a[1] * b[4] + a[2] * b[8] + a[3] * b[12],
      a[0] * b[1] + a[1] * b[5] + a[2] * b[9] + a[3] * b[13],
      a[0] * b[2] + a[1] * b[6] + a[2] * b[10] + a[3] * b[14],
      a[0] * b[3] + a[1] * b[7] + a[2] * b[11] + a[3] * b[15],
      a[4] * b[0] + a[5] * b[4] + a[6] * b[8] + a[7] * b[12],
      a[4] * b[1] + a[5] * b[5] + a[6] * b[9] + a[7] * b[13],
      a[4] * b[2] + a[5] * b[6] + a[6] * b[10] + a[7] * b[14],
      a[4] * b[3] + a[5] * b[7] + a[6] * b[11] + a[7] * b[15],
      a[8] * b[0] + a[9] * b[4] + a[10] * b[8] + a[11] * b[12],
      a[8] * b[1] + a[9] * b[5] + a[10] * b[9] + a[11] * b[13],
      a[8] * b[2] + a[9] * b[6] + a[10] * b[10] + a[11] * b[14],
      a[8] * b[3] + a[9] * b[7] + a[10] * b[11] + a[11] * b[15],
      a[12] * b[0] + a[13] * b[4] + a[14] * b[8] + a[15] * b[12],
      a[12] * b[1] + a[13] * b[5] + a[14] * b[9] + a[15] * b[13],
      a[12] * b[2] + a[13] * b[6] + a[14] * b[10] + a[15] * b[14],
      a[12] * b[3] + a[13] * b[7] + a[14] * b[11] + a[15] * b[15],
    });
  }

  public Vector transform(Vector point) {
    double oldX = point.getX();
    double oldY = point.getY();
    double oldZ = point.getZ();

    double x = matrix[0] * oldX + matrix[1] * oldY + matrix[2] * oldZ + matrix[3];
    double y = matrix[4] * oldX + matrix[5] * oldY + matrix[6] * oldZ + matrix[7];
    double z = matrix[8] * oldX + matrix[9] * oldY + matrix[10] * oldZ + matrix[11];
    return new Vector(x, y, z);
  }
}
