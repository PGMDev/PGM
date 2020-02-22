package tc.oc.pgm.portals;

public class RelativeDoubleProvider implements DoubleProvider {
  public static final RelativeDoubleProvider ZERO = new RelativeDoubleProvider(0);
  private final double delta;

  public RelativeDoubleProvider(double delta) {
    this.delta = delta;
  }

  @Override
  public double get(double old) {
    return old + this.delta;
  }

  public RelativeDoubleProvider inverse() {
    if (this.delta == 0) {
      return ZERO;
    } else {
      return new RelativeDoubleProvider(-this.delta);
    }
  }
}
