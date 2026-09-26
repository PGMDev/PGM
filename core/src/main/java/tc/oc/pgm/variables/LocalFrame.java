package tc.oc.pgm.variables;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LocalFrame {
  // Actions only ever run on the main thread
  private static final Deque<LocalFrame> STACK = new ArrayDeque<>();

  private final Layout layout;
  private final double[] values;

  public LocalFrame(Layout layout) {
    this(layout, new double[layout.size()]);
  }

  private LocalFrame(Layout layout, double[] values) {
    this.layout = layout;
    this.values = values;
  }

  // Binds a snapshot of the current frame, so later changes to locals are not seen by the runnable
  public static Runnable bind(Runnable runnable) {
    var frame = STACK.peek();
    if (frame == null) return runnable;
    var snapshot = new LocalFrame(frame.layout, frame.values.clone());
    return () -> run(snapshot, runnable);
  }

  public static void run(LocalFrame frame, Runnable runnable) {
    STACK.push(frame);
    try {
      runnable.run();
    } finally {
      STACK.pop();
    }
  }

  public static double[] values(Layout layout, String name) {
    LocalFrame frame = STACK.peek();
    if (frame == null || frame.layout != layout)
      throw new IllegalStateException("Local variable '" + name + "' used outside of its action");
    return frame.values;
  }

  public static final class Layout {
    private int size;

    int allocate() {
      return size++;
    }

    public int size() {
      return size;
    }
  }
}
