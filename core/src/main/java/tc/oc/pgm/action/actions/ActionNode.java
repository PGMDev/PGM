package tc.oc.pgm.action.actions;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.action.Action;

public class ActionNode<B> extends AbstractAction<B> {
  private final ImmutableList<Action<? super B>> actions;
  private final Class<B> bound;

  public ActionNode(ImmutableList<Action<? super B>> actions, Class<B> bound) {
    super(bound);
    this.actions = actions;
    this.bound = bound;
  }

  @Override
  public Class<B> getScope() {
    return bound;
  }

  @Override
  public void trigger(B t) {
    for (Action<? super B> action : actions) {
      action.trigger(t);
    }
  }
}
