package tc.oc.pgm.map;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdom2.Element;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

class ConditionalChecker {

  private static final List<AttributeCheck> ATTRIBUTES = List.of(
      new AttributeCheck("variant", ConditionalChecker::variant),
      new AttributeCheck("has-variant", ConditionalChecker::hasVariant),
      new AttributeCheck("constant", ConditionalChecker::constant),
      new AttributeCheck("min-server-version", ConditionalChecker::minServerVersion),
      new AttributeCheck("max-server-version", ConditionalChecker::maxServerVersion));
  private static final String ALL_ATTRS =
      ATTRIBUTES.stream().map(AttributeCheck::key).collect(Collectors.joining("', '", "'", "'"));

  /**
   * Test if the current context passes the conditions declared in the element
   *
   * @param ctx the map's context
   * @param el The conditional element
   * @return if the conditional passes
   * @throws InvalidXMLException if the element is invalid in any way
   */
  static boolean test(MapFilePreprocessor ctx, Element el) throws InvalidXMLException {
    Boolean result = null;
    for (var check : ATTRIBUTES) {
      Boolean attRes = check.apply(ctx, el);
      if (attRes != null) result = result == null ? attRes : result && attRes;
    }

    if (result != null) return result;
    throw new InvalidXMLException("Expected at least one of " + ALL_ATTRS + " attributes", el);
  }

  private static String[] split(String val) {
    return val.split("[\\s,]+");
  }

  private static boolean variant(MapFilePreprocessor ctx, Element el, Node node) {
    String value = node.getValue();
    if (value.indexOf(',') == -1) return value.equals(ctx.getVariant());
    return Set.of(split(value)).contains(ctx.getVariant());
  }

  private static boolean hasVariant(MapFilePreprocessor ctx, Element el, Node node) {
    String value = node.getValue();
    if (value.indexOf(',') == -1) return ctx.getVariantIds().contains(value);
    return Arrays.stream(split(value)).anyMatch(ctx.getVariantIds()::contains);
  }

  private static boolean minServerVersion(MapFilePreprocessor ctx, Element el, Node node)
      throws InvalidXMLException {
    return Platform.MINECRAFT_VERSION.isNoOlderThan(XMLUtils.parseSemanticVersion(node));
  }

  private static boolean maxServerVersion(MapFilePreprocessor ctx, Element el, Node node)
      throws InvalidXMLException {
    return Platform.MINECRAFT_VERSION.isNoNewerThan(XMLUtils.parseSemanticVersion(node));
  }

  private static boolean constant(MapFilePreprocessor ctx, Element el, Node node)
      throws InvalidXMLException {
    var id = node.getValue();
    var value = Node.fromAttr(el, "constant-value");
    var cmp = XMLUtils.parseEnum(
        Node.fromAttr(el, "constant-comparison"),
        Cmp.class,
        value == null ? Cmp.DEFINED : Cmp.EQUALS);

    var constants = ctx.getConstants();
    var isDefined = constants.containsKey(id);
    var constant = isDefined ? constants.get(id) : null;

    if (!cmp.requireValue && value != null)
      throw new InvalidXMLException("Comparison type " + cmp + " should not have a value", value);

    if (cmp.requireValue) {
      if (value == null)
        throw new InvalidXMLException("Required attribute 'constant-value' not set", el);

      if (!isDefined)
        throw new InvalidXMLException(
            "Unknown constant '" + id + "'. Only constants before the conditional may be used.",
            el);
      if (constant == null) return false;
    }

    // The only reason these are split is for the IDE to infer nullability
    if (!cmp.requireValue) {
      return switch (cmp) {
        case UNDEFINED -> !isDefined;
        case DEFINED -> isDefined;
        case DEFINED_DELETE -> isDefined && constant == null;
        case DEFINED_VALUE -> isDefined && constant != null;
        default -> throw new IllegalStateException("Unexpected value: " + cmp);
      };
    } else {
      return switch (cmp) {
        case EQUALS -> Objects.equals(value.getValue(), constant);
        case CONTAINS -> Set.of(split(value.getValue())).contains(constant);
        case REGEX -> constant.matches(value.getValue());
        case RANGE -> XMLUtils.parseNumericRange(value, Double.class)
            .contains(XMLUtils.parseNumber(new Node(el), constant, Double.class, true));
        default -> throw new IllegalStateException("Unexpected value: " + cmp);
      };
    }
  }

  enum Cmp {
    UNDEFINED(false),
    DEFINED(false),
    DEFINED_DELETE(false),
    DEFINED_VALUE(false),
    EQUALS(true),
    CONTAINS(true),
    REGEX(true),
    RANGE(true);
    private final boolean requireValue;

    Cmp(boolean requireValue) {
      this.requireValue = requireValue;
    }
  }

  interface ElementPredicate {
    boolean test(MapFilePreprocessor ctx, Element el, Node value) throws InvalidXMLException;
  }

  record AttributeCheck(String key, ElementPredicate pred) {
    Boolean apply(MapFilePreprocessor ctx, Element el) throws InvalidXMLException {
      var attr = Node.fromNullable(el.getAttribute(key));
      if (attr == null) return null;

      return pred.test(ctx, el, attr);
    }
  }
}
