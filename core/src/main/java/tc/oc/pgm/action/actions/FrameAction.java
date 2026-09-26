package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionDefinition;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.variables.LocalFrame;

public class FrameAction<S> implements ActionDefinition<S> {

  private final Action<S> action;
  private final LocalFrame.Layout layout;

  public FrameAction(Action<S> action, LocalFrame.Layout layout) {
    this.action = action;
    this.layout = layout;
  }

  @Override
  public Class<S> getScope() {
    return action.getScope();
  }

  @Override
  public void trigger(S s) {
    LocalFrame.run(new LocalFrame(layout), () -> action.trigger(s));
  }

  @Override
  public void trigger(S s, Query event) {
    LocalFrame.run(new LocalFrame(layout), () -> action.trigger(s, event));
  }

  @Override
  public void untrigger(S s) {
    LocalFrame.run(new LocalFrame(layout), () -> action.untrigger(s));
  }
}
