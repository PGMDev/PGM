package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.variables.LocalScope;

/**
 * Parse-time state threaded through nested actions: visible locals, control statements and, inside
 * a function, whether it returns a value.
 */
public record ActionParseContext(
    @Nullable LocalScope locals,
    ControlScope control,
    @Nullable Boolean returnsValue) {
  public static ActionParseContext root() {
    return new ActionParseContext(null, ControlScope.root(), null);
  }

  public static ActionParseContext function(LocalScope locals, boolean returnsValue) {
    return new ActionParseContext(locals, ControlScope.root(), returnsValue);
  }

  public ActionParseContext withLocals(@Nullable LocalScope locals) {
    return locals == null ? this : new ActionParseContext(locals, control, returnsValue);
  }

  public ActionParseContext loop() {
    return new ActionParseContext(locals, control.loop(), returnsValue);
  }

  public ActionParseContext withRootControl() {
    return new ActionParseContext(locals, ControlScope.root(), null);
  }
}
