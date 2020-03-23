package tc.oc.pgm.api.feature;

import tc.oc.pgm.api.match.Match;

/**
 * Super-class for all match-time features. Stores a unique ID (see documentation below) that is
 * unique across all elements. Can be referenced by querying against a {@link Match}'s {@link
 * tc.oc.pgm.features.MatchFeatureContext} after construction- time of the Match.
 */
public interface Feature<T extends FeatureDefinition> {

  /**
   * Returns the unique ID associated with this Feature. IDs are unique across all element types
   * (i.e., <core id="foo"/> and <destroyable id="foo"/> will cause a map to not be loaded). IDs may
   * be used as a unique reference point to gather an element for use in things where a name does
   * not suffice.
   */
  String getId();

  /** Return the {@link FeatureDefinition} instance from which this Feature was created */
  T getDefinition();
}
