package tc.oc.pgm.portals;

public class StaticDoubleProvider implements DoubleProvider {
  private final double value;

  public StaticDoubleProvider(double value) {
    this.value = value;
  }

  @Override
  public double get(double old) {
    return this.value;
  }
}
