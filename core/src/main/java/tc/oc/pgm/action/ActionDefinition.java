package tc.oc.pgm.action;

import tc.oc.pgm.api.feature.FeatureDefinition;

/** A {@link Action} that is not a reference */
public interface ActionDefinition<S> extends FeatureDefinition, Action<S> {}
