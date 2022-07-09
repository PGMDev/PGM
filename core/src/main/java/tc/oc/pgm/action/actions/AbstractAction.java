package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionDefinition;

public abstract class AbstractAction<S> implements ActionDefinition<S> {

  private final Class<S> cls;

  public AbstractAction(Class<S> cls) {
    this.cls = cls;
  }

  @Override
  public Class<S> getScope() {
    return cls;
  }

  @Override
  public void untrigger(S s) {}
}
