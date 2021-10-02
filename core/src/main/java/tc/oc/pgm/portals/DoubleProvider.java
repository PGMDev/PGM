package tc.oc.pgm.portals;

public interface DoubleProvider extends InvertibleOperator<DoubleProvider> {
  double apply(double old);

  boolean invertible();

  DoubleProvider inverse();
}
