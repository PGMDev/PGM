package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionDefinition;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

/**
 * Wraps an action definition to consider it exposed. This is an easy way to avoid needing each
 * specialized action to implement a way to expose itself or not.
 */
public class ExposedAction extends SelfIdentifyingFeatureDefinition
    implements ActionDefinition<Match>, Feature<ActionDefinition<? super Match>> {

  private final ActionDefinition<? super Match> delegate;

  public ExposedAction(String id, ActionDefinition<? super Match> delegate) {
    super(id);
    this.delegate = delegate;
  }

  @Override
  public ActionDefinition<? super Match> getDefinition() {
    return delegate;
  }

  @Override
  public Class<Match> getScope() {
    return Match.class;
  }

  @Override
  public void trigger(Match m) {
    delegate.trigger(m);
  }

  @Override
  public void untrigger(Match m) {
    delegate.untrigger(m);
  }
}
