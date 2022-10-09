package tc.oc.pgm.api.feature;

import org.jetbrains.annotations.Nullable;

/** A checked exception indicating a problem related to a particular {@link FeatureDefinition} */
public class FeatureDefinitionException extends Exception {

  private final FeatureDefinition featureDefinition;

  public FeatureDefinitionException(@Nullable String message, FeatureDefinition featureDefinition) {
    this(message, null, featureDefinition);
  }

  public FeatureDefinitionException(
      @Nullable String message, @Nullable Throwable cause, FeatureDefinition featureDefinition) {
    super(message, cause);
    this.featureDefinition = featureDefinition;
  }

  public FeatureDefinition featureDefinition() {
    return featureDefinition;
  }
}
