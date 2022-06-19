package tc.oc.pgm.trigger.triggers;

import tc.oc.pgm.trigger.TriggerDefinition;

public abstract class AbstractTrigger<S> implements TriggerDefinition<S> {

  private final Class<S> cls;

  public AbstractTrigger(Class<S> cls) {
    this.cls = cls;
  }

  @Override
  public Class<S> getScope() {
    return cls;
  }

  @Override
  public void untrigger(S s) {}
}
