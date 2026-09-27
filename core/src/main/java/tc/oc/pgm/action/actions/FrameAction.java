package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.variables.LocalScope;

public class FrameAction<S> extends AbstractAction<S> {

  private final Action<S> action;
  private final LocalScope scope;

  public FrameAction(Action<S> action, LocalScope scope) {
    super(action.getScope());
    this.action = action;
    this.scope = scope;
  }

  @Override
  public void trigger(S s, ActionContext context) {
    action.trigger(s, context.withLocals(scope.createFrame(context.locals())));
  }

  @Override
  public void untrigger(S s, ActionContext context) {
    action.untrigger(s, context.withLocals(scope.createFrame(context.locals())));
  }
}
