package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;

/**
 * Parse-time view of the control statements in a root action. Tracks whether statements are inside
 * a loop, and whether the root uses any so it only needs a {@link ControlFlow} when they are.
 */
public final class ControlScope {
  private final @Nullable ControlScope root;
  private final boolean loop;
  private boolean used;

  private ControlScope(@Nullable ControlScope root, boolean loop) {
    this.root = root;
    this.loop = loop;
  }

  public static ControlScope root() {
    return new ControlScope(null, false);
  }

  public ControlScope loop() {
    return loop ? this : new ControlScope(this, true);
  }

  public boolean inLoop() {
    return loop;
  }

  public void markUsed() {
    root(this).used = true;
  }

  public boolean isUsed() {
    return root(this).used;
  }

  private static ControlScope root(ControlScope scope) {
    return scope.root != null ? scope.root : scope;
  }
}
