package tc.oc.pgm.action.actions;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

public class ActionNode<B extends Filterable<?>> extends AbstractAction<B> {
  private final ImmutableList<Action<? super B>> actions;
  private final Filter filter;
  private final Filter untrigerFilter;
  private final Class<B> bound;

  public ActionNode(
      ImmutableList<Action<? super B>> actions,
      Filter filter,
      Filter untriggerFilter,
      Class<B> bound) {
    super(bound);
    this.actions = actions;
    this.filter = filter;
    this.untrigerFilter = untriggerFilter;
    this.bound = bound;
  }

  @Override
  public Class<B> getScope() {
    return bound;
  }

  @Override
  public void trigger(B t) {
    if (filter.query(t).isAllowed()) {
      for (Action<? super B> action : actions) {
        action.trigger(t);
      }
    }
  }

  public void untrigger(B t) {
    if (untrigerFilter.query(t).isAllowed()) {
      for (Action<? super B> action : actions) {
        action.untrigger(t);
      }
    }
  }
}
