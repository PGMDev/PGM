package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;

/**
 * Parse-time view of the control statements in a root action. Tracks whether statements are inside
 * a loop, and whether the root uses any so it only needs a {@link ControlFlow} when they are. A
 * function root also tracks whether any of its returns has a value.
 */
public final class ControlScope {
  private final @Nullable ControlScope root;
  private final boolean loop;
  private final boolean function;
  private boolean used;
  private boolean returnsValue;

  private ControlScope(@Nullable ControlScope root, boolean loop, boolean function) {
    this.root = root;
    this.loop = loop;
    this.function = function;
  }

  public static ControlScope root() {
    return new ControlScope(null, false, false);
  }

  public static ControlScope function() {
    return new ControlScope(null, false, true);
  }

  public ControlScope loop() {
    return loop ? this : new ControlScope(this, true, false);
  }

  public boolean inLoop() {
    return loop;
  }

  public boolean inFunction() {
    return root(this).function;
  }

  public void markUsed() {
    root(this).used = true;
  }

  public boolean isUsed() {
    return root(this).used;
  }

  public void markReturnsValue() {
    root(this).returnsValue = true;
  }

  public boolean returnsValue() {
    return root(this).returnsValue;
  }

  private static ControlScope root(ControlScope scope) {
    return scope.root != null ? scope.root : scope;
  }
}
