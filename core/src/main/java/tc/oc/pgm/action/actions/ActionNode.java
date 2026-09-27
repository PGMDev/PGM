package tc.oc.pgm.action.actions;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.LocalScope;

public class ActionNode<B extends Filterable<?>> extends AbstractAction<B> {
  private final ImmutableList<Action<? super B>> actions;
  private final Filter filter;
  private final Filter untrigerFilter;
  private final Class<B> bound;
  private final @Nullable LocalScope locals;

  public ActionNode(
      ImmutableList<Action<? super B>> actions,
      Filter filter,
      Filter untriggerFilter,
      Class<B> bound,
      @Nullable LocalScope locals) {
    super(bound);
    this.actions = actions;
    this.filter = filter;
    this.untrigerFilter = untriggerFilter;
    this.bound = bound;
    this.locals = locals;
  }

  @Override
  public Class<B> getScope() {
    return bound;
  }

  @Override
  public void trigger(B t, ActionContext context) {
    context = enterFrame(context);
    if (filter.query(context.queryOr(t), context.frame()).isAllowed()) {
      for (Action<? super B> action : actions) {
        action.trigger(t, context);
      }
    }
  }

  @Override
  public void untrigger(B t, ActionContext context) {
    context = enterFrame(context);
    if (untrigerFilter.query(t, context.frame()).isAllowed()) {
      for (Action<? super B> action : actions) {
        action.untrigger(t, context);
      }
    }
  }

  private ActionContext enterFrame(ActionContext context) {
    return locals == null ? context : context.withFrame(locals.createFrame(context.frame()));
  }
}
