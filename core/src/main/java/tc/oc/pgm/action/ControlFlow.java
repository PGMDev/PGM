package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;

/**
 * Runtime signal of a pending break, continue or return for a single root action execution, along
 * with the value of a function's return. Containers stop running children while halted, and loops
 * consume break and continue.
 */
public final class ControlFlow {
  public enum Signal {
    BREAK,
    CONTINUE,
    RETURN
  }

  private @Nullable Signal signal;
  private double value;

  public ControlFlow() {}

  public ControlFlow(double value) {
    this.value = value;
  }

  public void signal(Signal signal) {
    this.signal = signal;
  }

  public void returnValue(double value) {
    this.value = value;
    signal(Signal.RETURN);
  }

  public double value() {
    return value;
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
