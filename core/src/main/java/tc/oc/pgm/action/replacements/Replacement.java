package tc.oc.pgm.action.replacements;

import net.kyori.adventure.text.ComponentLike;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

/** A replacement object provides replacement text in message actions. */
public interface Replacement extends FeatureDefinition {
  /**
   * Tests if the replacement can be used with the passed filterable.
   *
   * @param scope The filterable to test
   * @param node The node reference used for parsing
   * @throws InvalidXMLException if the replacement cannot be used with the passed scope
   */
  default void validate(Class<? extends Filterable<?>> scope, Node node)
      throws InvalidXMLException {}

  /**
   * Creates a replacement component tailored to the given filterable.
   *
   * @param filterable The filterable to use when creating the replacement component
   * @return The replacement component
   */
  ComponentLike get(Filterable<?> filterable);
}
