package tc.oc.pgm.api.feature;

/**
 * Used as a base-class for all templates of features. Kept around during parsing time so that we
 * can easily convert a definition into a Feature for match-time, and persist the ID from the XML.
 * Also stored in the {@link tc.oc.pgm.features.FeatureDefinitionContext} so that we don't collide
 * IDs.
 */
public interface FeatureDefinition {}
