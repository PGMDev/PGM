package tc.oc.pgm.variables;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.math.LocalFrame;

public record LocalRef(String name, int depth, int slot) {

  public double get(@Nullable LocalFrame locals) {
    return frame(locals).get(slot);
  }

  public void set(@Nullable LocalFrame locals, double value) {
    frame(locals).set(slot, value);
  }

  private LocalFrame frame(@Nullable LocalFrame locals) {
    LocalFrame frame = locals == null ? null : locals.ancestor(depth);
    if (frame == null)
      throw new IllegalStateException("Local variable '" + name + "' used outside of its action");
    return frame;
  }
}
