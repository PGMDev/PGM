package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.variables.LocalScope;

/** Parse-time state threaded through nested actions: visible locals and control statements. */
public record ActionParseContext(@Nullable LocalScope locals, ControlScope control) {
  public static ActionParseContext root() {
    return new ActionParseContext(null, ControlScope.root());
  }

  public static ActionParseContext function(LocalScope locals) {
    return new ActionParseContext(locals, ControlScope.function());
  }

  public ActionParseContext withLocals(@Nullable LocalScope locals) {
    return locals == null ? this : new ActionParseContext(locals, control);
  }

  public ActionParseContext loop() {
    return new ActionParseContext(locals, control.loop());
  }

  public ActionParseContext withRootControl() {
    return new ActionParseContext(locals, ControlScope.root());
  }
}
