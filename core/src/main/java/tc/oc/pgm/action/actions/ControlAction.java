package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.ControlFlow;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

public class ControlAction extends AbstractAction<Filterable> {

  private final ControlFlow.Signal signal;
  private final Filter filter;

  public ControlAction(ControlFlow.Signal signal, Filter filter) {
    super(Filterable.class);
    this.signal = signal;
    this.filter = filter;
  }

  @Override
  public void trigger(Filterable t, ActionContext context) {
    if (!filter.query(context.queryOr(t), context.frame()).isAllowed()) return;
    if (context.flow() == null)
      throw new IllegalStateException("Control statement ran outside of its root action");
    context.flow().signal(signal);
  }
}
