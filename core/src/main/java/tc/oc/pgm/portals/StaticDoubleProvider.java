package tc.oc.pgm.portals;

public class StaticDoubleProvider implements DoubleProvider {
  private final double value;

  public StaticDoubleProvider(double value) {
    this.value = value;
  }

  @Override
  public double apply(double old) {
    return this.value;
  }

  @Override
  public boolean invertible() {
    return false;
  }

  @Override
  public DoubleProvider inverse() {
    return null;
  }
}
