package tc.oc.pgm.trigger.triggers;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.trigger.Trigger;

public class TriggerNode<B> extends AbstractTrigger<B> {
  private final ImmutableList<Trigger<? super B>> triggers;
  private final Class<B> bound;

  public TriggerNode(ImmutableList<Trigger<? super B>> triggers, Class<B> bound) {
    super(bound);
    this.triggers = triggers;
    this.bound = bound;
  }

  @Override
  public Class<B> getScope() {
    return bound;
  }

  @Override
  public void trigger(B t) {
    for (Trigger<? super B> trigger : triggers) {
      trigger.trigger(t);
    }
  }
}
