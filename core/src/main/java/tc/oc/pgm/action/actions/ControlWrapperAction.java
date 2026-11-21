package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.ControlFlow;

/**
 * Wraps a root action that uses break, continue or return, giving each execution its own
 * {@link ControlFlow}.
 */
public class ControlWrapperAction<S> extends AbstractAction<S> {

  private final Action<S> action;

  private ControlWrapperAction(Action<S> action) {
    super(action.getScope());
    this.action = action;
  }

  public static <S> ControlWrapperAction<S> of(Action<S> action) {
    return new ControlWrapperAction<>(action);
  }

  @Override
  public void trigger(S s, ActionContext context) {
    action.trigger(s, context.withFlow(new ControlFlow()));
  }

  @Override
  public void untrigger(S s, ActionContext context) {
    action.untrigger(s, context);
  }
}
