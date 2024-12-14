package tc.oc.pgm.action.replacements;

import net.kyori.adventure.text.ComponentLike;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.filters.Filterable;

/** A replacement object provides replacement text in message actions. */
public interface Replacement extends FeatureDefinition {
  /**
   * Tests if the replacement can be used with the passed filterable.
   *
   * @param filterable The filterable to test
   * @return Whether the replacement can be used with the passed filterable
   */
  default boolean canUse(Class<? extends Filterable<?>> filterable) {
    return true;
  }

  /**
   * Creates a replacement component tailored to the given filterable.
   *
   * @param filterable The filterable to use when creating the replacement component
   * @return The replacement component
   */
  ComponentLike get(Filterable<?> filterable);
}
