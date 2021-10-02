package tc.oc.pgm.portals;

public interface InvertibleOperator<S extends InvertibleOperator<S>> {

  boolean invertible();

  default S inverse() {
    throw new UnsupportedOperationException(getClass().getName() + " is not invertible");
  }
}
