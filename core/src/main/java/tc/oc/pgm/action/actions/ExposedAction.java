package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionDefinition;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;

/**
 * Wraps an action definition to consider it exposed. This is an easy way to avoid needing each
 * specialized action to implement a way to expose itself or not.
 */
public class ExposedAction extends SelfIdentifyingFeatureDefinition
    implements ActionFeature<Filterable<?>> {

  private final ActionDefinition<Filterable<?>> delegate;

  public ExposedAction(String id, ActionDefinition<Filterable<?>> delegate) {
    super(id);
    this.delegate = delegate;
  }

  @Override
  public ActionDefinition<Filterable<?>> getDefinition() {
    return delegate;
  }

  @Override
  public Class<Filterable<?>> getScope() {
    return delegate.getScope();
  }

  @Override
  public void trigger(Filterable<?> scope) {
    delegate.trigger(getAncestor(scope));
  }

  @Override
  public void untrigger(Filterable<?> scope) {
    delegate.untrigger(getAncestor(scope));
  }

  protected Filterable<?> getAncestor(Filterable<?> context) {
    Filterable<?> filterable = context.getFilterableAncestor(delegate.getScope());
    if (filterable != null) return filterable;

    throw new IllegalStateException(
        "Wrong exposed scope for '"
            + getId()
            + "', expected "
            + delegate.getScope().getSimpleName()
            + " which cannot be found in "
            + context.getClass().getSimpleName());
  }
}

interface ActionFeature<S> extends Feature<ActionDefinition<S>>, ActionDefinition<S> {}
