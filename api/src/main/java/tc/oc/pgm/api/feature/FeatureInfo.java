package tc.oc.pgm.api.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Supplies meta-information for {@link tc.oc.pgm.api.feature.FeatureDefinition}s */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FeatureInfo {
  /** A readable name for the type of feature (used for database documents and error messages) */
  public String name();
}
