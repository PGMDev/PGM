package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;

/**
 * Runtime signal of a pending break, continue or return for a single root action execution.
 * Containers stop running children while halted, and loops consume break and continue.
 */
public final class ControlFlow {
  public enum Signal {
    BREAK,
    CONTINUE,
    RETURN
  }

  private @Nullable Signal signal;

  public void signal(Signal signal) {
    this.signal = signal;
  }

  public boolean halted() {
    return signal != null;
  }

  public boolean haltLoop() {
    if (signal == Signal.RETURN) return true;
    boolean halt = signal == Signal.BREAK;
    signal = null;
    return halt;
  }
}
