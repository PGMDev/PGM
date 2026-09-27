package tc.oc.pgm.util.math;

import org.jetbrains.annotations.Nullable;

public final class LocalFrame {
  private final @Nullable LocalFrame parent;
  private final double[] values;

  public LocalFrame(int size, @Nullable LocalFrame parent) {
    this.parent = parent;
    this.values = new double[size];
  }

  public double get(LocalRef ref) {
    return frame(ref).values[ref.slot()];
  }

  public void set(LocalRef ref, double value) {
    frame(ref).values[ref.slot()] = value;
  }

  private LocalFrame frame(LocalRef ref) {
    var frame = this;
    for (int i = 0; i < ref.depth(); i++) frame = frame.parent;
    return frame;
  }
}
