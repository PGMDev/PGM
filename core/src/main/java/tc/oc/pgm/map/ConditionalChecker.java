package tc.oc.pgm.map;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

class ConditionalChecker {

  private static final List<AttributeCheck> ATTRIBUTES = List.of(
      new AttributeCheck("variant", ConditionalChecker::variant),
      new AttributeCheck("has-variant", ConditionalChecker::hasVariant),
      new AttributeCheck("constant", ConditionalChecker::constant));
  private static final String ALL_ATTRS =
      ATTRIBUTES.stream().map(AttributeCheck::key).collect(Collectors.joining("', '", "'", "'"));

  /**
   * Test if the current context passes the conditions declared in the element
   *
   * @param ctx the map's context
   * @param el The conditional element
   * @return true if the conditional
   * @throws InvalidXMLException if the element is invalid in any way
   */
  static boolean test(MapFilePreprocessor ctx, Element el) throws InvalidXMLException {
    Boolean result = null;
    for (var check : ATTRIBUTES) {
      Boolean attRes = check.apply(ctx, el);
      if (attRes != null) result = result == null ? attRes : result && attRes;
    }

    if (result == null)
      throw new InvalidXMLException("Expected at least one of " + ALL_ATTRS + " attributes", el);

    return result;
  }

  private static String[] split(String val) {
    return val.split("[\\s,]+");
  }

  private static boolean variant(MapFilePreprocessor ctx, Element el, String value) {
    if (value.indexOf(',') == -1) return value.equals(ctx.getVariant());
    return Set.of(split(value)).contains(ctx.getVariant());
  }

  private static boolean hasVariant(MapFilePreprocessor ctx, Element el, String value) {
    if (value.indexOf(',') == -1) return ctx.getVariantIds().contains(value);
    return Arrays.stream(split(value)).anyMatch(ctx.getVariantIds()::contains);
  }

  private static boolean constant(MapFilePreprocessor ctx, Element el, String id)
      throws InvalidXMLException {
    var value = Node.fromAttr(el, "constant-value");
    var cmp = XMLUtils.parseEnum(
        Node.fromAttr(el, "constant-comparison"),
        Cmp.class,
        value == null ? Cmp.DEFINED : Cmp.EQUALS);

    var constants = ctx.getConstants();

    if (!cmp.requireValue) {
      if (value != null)
        throw new InvalidXMLException("Comparison type " + cmp + " should not have a value", value);

      if (!constants.containsKey(id)) return cmp == Cmp.UNDEFINED;

      return switch (cmp) {
        case DEFINED -> true;
        case DEFINED_DELETE -> constants.get(id) == null;
        case DEFINED_VALUE -> constants.get(id) != null;
          // Should never happen
        default -> throw new IllegalStateException(cmp + " not supported");
      };
    } else {
      String constant = constants.get(id);
      if (constant == null) {
        if (!constants.containsKey(id))
          throw new InvalidXMLException(
              "Unknown constant '" + id + "'. Only constants before the conditional may be used.",
              el);
        return false;
      }
      if (value == null)
        throw new InvalidXMLException("Required attribute 'constant-value' not set", el);

      return switch (cmp) {
        case EQUALS -> Objects.equals(value.getValue(), constant);
        case CONTAINS -> Set.of(split(value.getValue())).contains(constant);
        case REGEX -> constant.matches(value.getValue());
        case RANGE -> XMLUtils.parseNumericRange(value, Double.class)
            .contains(XMLUtils.parseNumber(new Node(el), constant, Double.class, true));
          // Should never happen
        default -> throw new IllegalStateException(cmp + " not supported");
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
    boolean test(MapFilePreprocessor ctx, Element el, String value) throws InvalidXMLException;
  }

  record AttributeCheck(String key, ElementPredicate pred) {
    Boolean apply(MapFilePreprocessor ctx, Element el) throws InvalidXMLException {
      var attr = el.getAttribute(key);
      if (attr == null) return null;

      return pred.test(ctx, el, attr.getValue());
    }
  }
}
