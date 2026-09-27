package tc.oc.pgm.util.math;

import org.jetbrains.annotations.Nullable;

public final class LocalFrame {
  private final @Nullable LocalFrame parent;
  private final double[] values;

  public LocalFrame(int size, @Nullable LocalFrame parent) {
    this.parent = parent;
    this.values = new double[size];
  }

  public double get(int slot) {
    return values[slot];
  }

  public void set(int slot, double value) {
    values[slot] = value;
  }

  public @Nullable LocalFrame ancestor(int depth) {
    var frame = this;
    for (int i = 0; i < depth && frame != null; i++) frame = frame.parent;
    return frame;
  }
}
