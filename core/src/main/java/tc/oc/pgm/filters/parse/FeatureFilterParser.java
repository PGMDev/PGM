package tc.oc.pgm.filters.parse;

import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.XMLFilterReference;
import tc.oc.pgm.filters.operator.AllowFilter;
import tc.oc.pgm.filters.operator.DenyFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class FeatureFilterParser extends FilterParser {

  public FeatureFilterParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public FeatureDefinitionContext getUsedContext() {
    return factory.getFeatures();
  }

  @Override
  public Filter parse(Element el) throws InvalidXMLException {
    Filter filter = this.parseDynamic(el);
    if (!(filter instanceof FeatureReference)) {
      factory.getFeatures().addFeature(el, filter);
    }
    return filter;
  }

  @Override
  public Filter parseReference(Node node, String value) throws InvalidXMLException {
    return factory
        .getFeatures()
        .addReference(
            new XMLFilterReference(factory.getFeatures(), node, value, FilterDefinition.class));
  }

  @Override
  public void validate(Filter filter, FeatureValidation<FilterDefinition> validation, Node node)
      throws InvalidXMLException {
    if (filter instanceof XMLFilterReference) {
      factory.getFeatures().validate((XMLFilterReference) filter, validation);
    } else if (filter instanceof FilterDefinition) {
      factory.getFeatures().validate((FilterDefinition) filter, validation, node);
    } else {
      throw new IllegalStateException(
          "Attempted validation on a filter which is neither definition nor reference.");
    }
  }

  @MethodParser("filter")
  public Filter parseFilter(Element el) throws InvalidXMLException {
    return parseReference(Node.fromRequiredAttr(el, "id"));
  }

  @MethodParser("allow")
  public Filter parseAllow(Element el) throws InvalidXMLException {
    return new AllowFilter(parseChild(el));
  }

  @MethodParser("deny")
  public Filter parseDeny(Element el) throws InvalidXMLException {
    return new DenyFilter(parseChild(el));
  }

  // Override with a version that only accepts a single child filter
  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(parseChild(el));
  }
}
