package tc.oc.pgm.filters.parse;

import com.google.common.collect.Range;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.XMLFilterReference;
import tc.oc.pgm.filters.matcher.match.VariableFilter;
import tc.oc.pgm.filters.operator.AllowFilter;
import tc.oc.pgm.filters.operator.DenyFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.VariableDefinition;
import tc.oc.pgm.variables.VariablesModule;

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
  public Filter parseReference(Node node, String id) throws InvalidXMLException {
    Filter inline = parseInlineFilter(node, id);
    if (inline != null) return inline;

    return features.addReference(new XMLFilterReference(factory.getFeatures(), node, id));
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

  private static final Pattern INLINE_VARIABLE =
      Pattern.compile(
          "("
              + VariablesModule.Factory.VARIABLE_ID.pattern()
              + ")\\s*=\\s*("
              + XMLUtils.RANGE_DOTTED.pattern()
              + ")");

  private @Nullable Filter parseInlineFilter(Node node, String text) throws InvalidXMLException {
    // Parse variable filter
    Matcher varMatch = INLINE_VARIABLE.matcher(text);
    if (varMatch.matches()) {
      VariableDefinition<?> variable =
          features.resolve(node, varMatch.group(1), VariableDefinition.class);
      Range<Double> range = XMLUtils.parseNumericRange(node, varMatch.group(2), Double.class);
      return new VariableFilter(variable, range);
    }

    // TODO: Future implementations for all/and (&), any/or (|), not (!), deny (~)

    return null;
  }
}
