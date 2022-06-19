package tc.oc.pgm.trigger;

import tc.oc.pgm.api.feature.FeatureDefinition;

/** A {@link Trigger} that is not a reference */
public interface TriggerDefinition<S> extends FeatureDefinition, Trigger<S> {}
