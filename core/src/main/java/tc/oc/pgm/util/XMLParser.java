package tc.oc.pgm.util;

import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public interface XMLParser<F, FD extends FeatureDefinition> {

  String type();

  /**
   * Parse this element as directly an instance of the feature
   *
   * @param el The element to parse
   * @return The parsed feature
   * @throws InvalidXMLException If the xml is invalid
   */
  F parse(Element el) throws InvalidXMLException;

  /**
   * Parse the element as a property, parsing the child or children in it. Behavior may differ,
   * filters enforce single child while regions wrap as union.
   *
   * @param property The parent element
   * @return The parsed feature
   * @throws InvalidXMLException If the xml is invalid
   */
  F parsePropertyElement(Element property) throws InvalidXMLException;

  default F parseReference(Node node) throws InvalidXMLException {
    return parseReference(node, node.getValue());
  }

  F parseReference(Node node, String id) throws InvalidXMLException;

  void validate(F f, FeatureValidation<FD> validation, Node node) throws InvalidXMLException;

  default F parseChild(Element parent) throws InvalidXMLException {
    if (parent.getChildren().isEmpty()) {
      throw new InvalidXMLException("Expected a child " + type(), parent);
    } else if (parent.getChildren().size() > 1) {
      throw new InvalidXMLException("Expected only one child " + type() + ", not multiple", parent);
    }
    return this.parse(parent.getChildren().get(0));
  }

  /**
   * Parse the element as a property, this means either an attribute reference, or a child element
   * with that name.
   *
   * @param el The parent element
   * @param name The property name
   * @return A parsed instance of the feature, or a reference
   * @throws InvalidXMLException If the xml is invalid
   */
  default @Nullable F parseProperty(Element el, String name) throws InvalidXMLException {
    return this.parseProperty(el, name, null, null);
  }

  default F parseProperty(Element el, String name, @Nullable F def) throws InvalidXMLException {
    return parseProperty(el, name, def, null);
  }

  default F parseProperty(Element el, String name, @Nullable FeatureValidation<FD> validation)
      throws InvalidXMLException {
    return parseProperty(el, name, null, validation);
  }

  default F parseProperty(
      Element el, String name, @Nullable F def, @Nullable FeatureValidation<FD> validation)
      throws InvalidXMLException {
    return parseProperty(Node.fromChildOrAttr(el, name), def, validation);
  }

  default F parseProperty(Node node) throws InvalidXMLException {
    return parseProperty(node, null, null);
  }

  default F parseProperty(Node node, @Nullable F def) throws InvalidXMLException {
    return parseProperty(node, def, null);
  }

  default F parseProperty(Node node, @Nullable FeatureValidation<FD> validation)
      throws InvalidXMLException {
    return parseProperty(node, null, validation);
  }

  default F parseProperty(Node node, @Nullable F def, @Nullable FeatureValidation<FD> validation)
      throws InvalidXMLException {
    if (node == null) return def;
    F feature =
        node.isAttribute()
            ? this.parseReference(node)
            : this.parsePropertyElement(node.getElement());

    if (validation != null) validate(feature, validation, node);
    return feature;
  }

  default F parseRequiredProperty(Element el, String name) throws InvalidXMLException {
    return parseRequiredProperty(el, name, null);
  }

  default F parseRequiredProperty(
      Element el, String name, @Nullable FeatureValidation<FD> validation)
      throws InvalidXMLException {
    F feature = this.parseProperty(el, name, null, validation);
    if (feature == null)
      throw new InvalidXMLException("Missing required " + type() + " '" + name + "'", el);
    return feature;
  }
}
