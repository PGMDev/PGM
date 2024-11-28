package tc.oc.pgm.util.xml;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;
import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.attribute.Attributes;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.DyeColors;
import tc.oc.pgm.util.bukkit.Enchantments;
import tc.oc.pgm.util.bukkit.PotionEffects;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.matcher.AllMaterialMatcher;
import tc.oc.pgm.util.material.matcher.BlockMaterialMatcher;
import tc.oc.pgm.util.math.OffsetVector;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.range.Ranges;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class XMLUtils {
  private XMLUtils() {}

  /**
   * Generates a list of child elements from a parent / child tree.
   *
   * @param root Root element to start with.
   * @param parentTagNames Parent element names.
   * @param childTagNames Child element names. All elements returned will have one of these names.
   *     If this is null then all non-parent elements found are included as children.
   * @param minChildDepth Minimum number of parent tags that children must be wrapped in. If this is
   *     zero then children of the root tag can be included in the result, outside of any parent
   *     tag.
   * @return List of child elements in the tree, in the order they appear, with attributes inherited
   *     from all of their ancestors.
   */
  public static List<Element> flattenElements(
      Element root,
      Set<String> parentTagNames,
      @Nullable Set<String> childTagNames,
      int minChildDepth) {
    // Walk the tree in-order to preserve the child ordering
    List<Element> result = Lists.newArrayList();

    InheritingElement el = (InheritingElement) root;

    for (Element child : minChildDepth > 0
        ? el.getChildren(parentTagNames)
        : childTagNames == null
            ? root.getChildren()
            : el.getChildren(Sets.union(parentTagNames, childTagNames))) {
      if (parentTagNames.contains(child.getName())) {
        result.addAll(flattenElements(
            new InheritingElement(child), parentTagNames, childTagNames, minChildDepth - 1));
      } else {
        result.add(new InheritingElement(child));
      }
    }
    return result;
  }

  public static List<Element> flattenElements(
      Element root, Set<String> parentTagNames, @Nullable Set<String> childTagNames) {
    return flattenElements(root, parentTagNames, childTagNames, 1);
  }

  public static List<Element> flattenElements(Element root, Set<String> parentTagNames) {
    return flattenElements(root, parentTagNames, null);
  }

  public static List<Element> flattenElements(
      Element root, String parentTagName, @Nullable String childTagName, int minChildDepth) {
    return flattenElements(
        root,
        ImmutableSet.of(parentTagName),
        childTagName == null ? null : ImmutableSet.of(childTagName),
        minChildDepth);
  }

  public static List<Element> flattenElements(
      Element root, String parentTagName, @Nullable String childTagName) {
    return flattenElements(root, parentTagName, childTagName, 1);
  }

  public static List<Element> flattenElements(Element root, String parentTagName) {
    return flattenElements(root, parentTagName, null);
  }

  public static Iterable<Element> getChildren(Element parent, String... names) {
    final Set<String> nameSet = new HashSet<>(Arrays.asList(names));
    return Iterables.filter(parent.getChildren(), new Predicate<Element>() {
      @Override
      public boolean apply(Element child) {
        return nameSet.contains(child.getName());
      }
    });
  }

  public static Iterable<Attribute> getAttributes(Element parent, String... names) {
    final Set<String> nameSet = new HashSet<>(Arrays.asList(names));
    return Iterables.filter(parent.getAttributes(), new Predicate<Attribute>() {
      @Override
      public boolean apply(Attribute child) {
        return nameSet.contains(child.getName());
      }
    });
  }

  public static @Nullable Attribute getAttribute(Element parent, String... names) {
    for (String name : names) {
      final Attribute attr = parent.getAttribute(name);
      if (attr != null) return attr;
    }
    return null;
  }

  public static Element getUniqueChild(Element parent, String... aliases)
      throws InvalidXMLException {
    List<Element> children = new ArrayList<>();
    for (String alias : aliases) {
      children.addAll(parent.getChildren(alias));
    }

    if (children.size() > 1) {
      throw new InvalidXMLException("multiple '" + aliases[0] + "' tags not allowed", parent);
    }
    return children.isEmpty() ? null : children.get(0);
  }

  public static Element getRequiredUniqueChild(Element parent, String... aliases)
      throws InvalidXMLException {
    List<Element> children = new ArrayList<>();
    for (String alias : aliases) {
      children.addAll(parent.getChildren(alias));
    }

    if (children.size() > 1) {
      throw new InvalidXMLException("multiple '" + aliases[0] + "' tags not allowed", parent);
    } else if (children.isEmpty()) {
      throw new InvalidXMLException("child tag '" + aliases[0] + "' is required", parent);
    }
    return children.get(0);
  }

  public static Attribute getRequiredAttribute(Element el, String... aliases)
      throws InvalidXMLException {
    Attribute attr = null;
    for (String alias : aliases) {
      Attribute a = el.getAttribute(alias);
      if (a != null) {
        if (attr == null) {
          attr = a;
        } else {
          throw new InvalidXMLException(
              "attributes '"
                  + attr.getName()
                  + "' and '"
                  + alias
                  + "' are aliases for the same thing, and cannot be combined",
              el);
        }
      }
    }

    if (attr == null) {
      throw new InvalidXMLException("attribute '" + aliases[0] + "' is required", el);
    }

    return attr;
  }

  private static Boolean parseBoolean(Node node, String value) throws InvalidXMLException {
    try {
      return TextParser.parseBoolean(value);
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static Boolean parseBoolean(Node node) throws InvalidXMLException {
    return node == null ? null : parseBoolean(node, node.getValue());
  }

  public static Boolean parseBoolean(@Nullable Node node, Boolean def) throws InvalidXMLException {
    return node == null ? def : parseBoolean(node);
  }

  public static Boolean parseBoolean(@Nullable Element el, Boolean def) throws InvalidXMLException {
    return el == null ? def : parseBoolean(new Node(el));
  }

  public static Boolean parseBoolean(@Nullable Attribute attr, Boolean def)
      throws InvalidXMLException {
    return attr == null ? def : parseBoolean(new Node(attr));
  }

  public static String parseRequiredId(Element element) throws InvalidXMLException {
    String id = Node.fromRequiredAttr(element, "id").getValue();

    if (id == null || id.isEmpty())
      throw new InvalidXMLException("Id attribute cannot be empty", element);

    return id;
  }

  /**
   * Get the value of the given numeric type that best represents positive infinity.
   *
   * @throws ReflectiveOperationException if this fails, which should not happen with the primitive
   *     types
   */
  private static <T extends Number> T positiveInfinity(Class<T> type)
      throws ReflectiveOperationException {
    try {
      return type.cast(type.getField("POSITIVE_INFINITY").get(null));
    } catch (NoSuchFieldException e) {
      return type.cast(type.getField("MAX_VALUE").get(null));
    }
  }

  /**
   * Get the value of the given numeric type that best represents negative infinity.
   *
   * @throws ReflectiveOperationException if this fails, which should not happen with the primitive
   *     types
   */
  private static <T extends Number> T negativeInfinity(Class<T> type)
      throws ReflectiveOperationException {
    try {
      return type.cast(type.getField("NEGATIVE_INFINITY").get(null));
    } catch (NoSuchFieldException e) {
      return type.cast(type.getField("MIN_VALUE").get(null));
    }
  }

  /**
   * Try to parse the given text as a number of the given type
   *
   * @param text string representation of a number
   * @param type numeric type to parse
   * @param infinity whether infinities should be allowed
   * @return a parsed number
   * @throws NumberFormatException if a number could not be parsed for whatever reason
   */
  public static <T extends Number> T parseNumber(String text, Class<T> type, boolean infinity)
      throws NumberFormatException {
    try {
      if (infinity) {
        String trimmed = text.trim();
        if ("oo".equals(trimmed) || "+oo".equals(trimmed)) {
          return positiveInfinity(type);
        } else if ("-oo".equals(trimmed)) {
          return negativeInfinity(type);
        }
      }
      return type.cast(type.getMethod("valueOf", String.class).invoke(null, text));
    } catch (ReflectiveOperationException e) {
      if (e.getCause() instanceof NumberFormatException) {
        throw (NumberFormatException) e.getCause();
      } else {
        throw new IllegalArgumentException("cannot parse type " + type.getName(), e);
      }
    }
  }

  public static <T extends Number> T parseNumber(
      Node node, String text, Class<T> type, boolean infinity) throws InvalidXMLException {
    try {
      return parseNumber(text, type, infinity);
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid number '" + text + "'", node);
    }
  }

  public static <T extends Number> T parseNumber(
      Node node, String text, Class<T> type, boolean infinity, T def) throws InvalidXMLException {
    return node == null ? def : parseNumber(node, text, type, infinity);
  }

  public static <T extends Number> T parseNumber(Node node, String text, Class<T> type, T def)
      throws InvalidXMLException {
    return parseNumber(node, text, type, false, def);
  }

  public static <T extends Number> T parseNumber(Node node, String text, Class<T> type)
      throws InvalidXMLException {
    return parseNumber(node, text, type, false);
  }

  public static <T extends Number> T parseNumber(Node node, Class<T> type, boolean infinity)
      throws InvalidXMLException {
    return parseNumber(node, node.getValue(), type, infinity);
  }

  public static <T extends Number> T parseNumber(Node node, Class<T> type)
      throws InvalidXMLException {
    return parseNumber(node, node.getValue(), type);
  }

  public static <T extends Number> T parseNumber(Attribute attr, Class<T> type)
      throws InvalidXMLException {
    return parseNumber(new Node(attr), type);
  }

  public static <T extends Number> T parseNumber(Element el, Class<T> type)
      throws InvalidXMLException {
    return parseNumber(new Node(el), type);
  }

  public static <T extends Number> T parseNumber(Node node, Class<T> type, boolean infinity, T def)
      throws InvalidXMLException {
    if (node == null) {
      return def;
    } else {
      return parseNumber(node, node.getValue(), type, infinity);
    }
  }

  public static <T extends Number> T parseNumber(Node node, Class<T> type, T def)
      throws InvalidXMLException {
    return parseNumber(node, type, false, def);
  }

  public static <T extends Number> T parseNumber(Element el, Class<T> type, T def)
      throws InvalidXMLException {
    if (el == null) {
      return def;
    } else {
      return parseNumber(el, type);
    }
  }

  public static <T extends Number> T parseNumber(Attribute attr, Class<T> type, T def)
      throws InvalidXMLException {
    if (attr == null) {
      return def;
    } else {
      return parseNumber(attr, type);
    }
  }

  public static <T extends Number & Comparable<T>> T parseNumber(
      Element el, Class<T> type, Range<T> range) throws InvalidXMLException {
    return parseNumberInRange(new Node(el), type, range);
  }

  public static <T extends Number & Comparable<T>> T parseNumber(
      Attribute attr, Class<T> type, Range<T> range) throws InvalidXMLException {
    return parseNumberInRange(new Node(attr), type, range);
  }

  public static <T extends Number & Comparable<T>> T parseNumberInRange(
      Node node, Class<T> type, Range<T> range) throws InvalidXMLException {
    T value = parseNumber(node, type);
    if (!range.contains(value)) {
      throw new InvalidXMLException(value + " is not in the range " + range, node);
    }
    return value;
  }

  public static <T extends Number & Comparable<T>> T parseNumberInRange(
      Node node, Class<T> type, Range<T> range, T def) throws InvalidXMLException {
    if (node == null) return def;
    else return parseNumberInRange(node, type, range);
  }

  public static <T extends Number & Comparable<T>> T parseNumberInRange(
      Node node, String text, Class<T> type, Range<T> range) throws InvalidXMLException {
    T value = parseNumber(node, text, type);
    if (!range.contains(value)) {
      throw new InvalidXMLException(value + " is not in the range " + range, node);
    }
    return value;
  }

  private static final Pattern RANGE_RE =
      Pattern.compile("\\s*(\\(|\\[)\\s*([^,]+)\\s*,\\s*([^\\)\\]]+)\\s*(\\)|\\])\\s*");

  public static final Pattern RANGE_DOTTED =
      Pattern.compile("\\s*(-oo|-?\\d*\\.?\\d+)?\\s*\\.\\.\\s*(oo|-?\\d*\\.?\\d+)?\\s*");

  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      @Nullable Node node, Class<T> type, @Nullable Range<T> fallback) throws InvalidXMLException {
    if (node == null) return fallback;
    return parseNumericRange(node, type);
  }

  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      @NotNull Node node, Class<T> type) throws InvalidXMLException {
    return parseNumericRange(node, node.getValue(), type);
  }

  /**
   * Parse a range in multiple formats.
   *
   * <p>Standard interval mathematical format: [0, 1) for a close-open range from 0 to 1
   *
   * <p>Singleton range (a standalone number)
   *
   * <p>Vanilla minecraft dotted range notation e.g.
   *
   * <p>1..5 or ..5 or 1..
   *
   * <p>equal to [1, 5], (-oo, 5] and [1, oo)
   *
   * @implNote Since infinity and "infinity"({@link Double#POSITIVE_INFINITY} etc.) is handled
   *     differently by the Google ranges we find the infinities and create Ranges using
   *     {@link Range#upTo(Comparable, BoundType) Range.upTo} and {@link Range#downTo(Comparable,
   *     BoundType) Range.downTo} instead of resolving to the max or min value of the range type.
   *     (Like {@link #parseNumber(String, Class, boolean)} does)
   */
  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Node node, String nodeValue, Class<T> type) throws InvalidXMLException {

    String lowStr;
    BoundType lowerBound;
    String uppStr;
    BoundType upperBound;

    Matcher matcher;
    if ((matcher = RANGE_DOTTED.matcher(nodeValue)).matches()) {
      lowStr = matcher.group(1);
      uppStr = matcher.group(2);
      lowerBound = BoundType.CLOSED;
      upperBound = BoundType.CLOSED;
    } else if ((matcher = RANGE_RE.matcher(nodeValue)).matches()) {
      lowStr = matcher.group(2);
      uppStr = matcher.group(3);
      lowerBound = "(".equals(matcher.group(1)) ? BoundType.OPEN : BoundType.CLOSED;
      upperBound = ")".equals(matcher.group(4)) ? BoundType.OPEN : BoundType.CLOSED;
    } else {
      // Try to parse as singleton range
      T value = parseNumber(node, nodeValue, type, true);
      if (value != null) {
        return Range.singleton(value);
      }
      throw new InvalidXMLException(
          "Invalid " + type.getSimpleName().toLowerCase() + " range '" + nodeValue + "'", node);
    }

    T lower =
        lowStr == null || lowStr.equals("-oo") ? null : parseNumber(node, lowStr, type, false);
    T upper = uppStr == null || uppStr.equals("oo") ? null : parseNumber(node, uppStr, type, false);

    return parseRange(node, lower, lowerBound, upper, upperBound);
  }

  public static <T extends Comparable<T>> Range<T> parseClosedRange(
      Node node, @Nullable T lower, @Nullable T upper) throws InvalidXMLException {
    return parseRange(node, lower, BoundType.CLOSED, upper, BoundType.CLOSED);
  }

  public static <T extends Comparable<T>> Range<T> parseRange(
      Node node, @Nullable T lower, BoundType lowerBound, @Nullable T upper, BoundType upperBound)
      throws InvalidXMLException {
    if (lower != null && upper != null) {
      if (lower.compareTo(upper) > 0) {
        throw new InvalidXMLException(
            "range lower bound (" + lower + ") cannot be greater than upper bound (" + upper + ")",
            node);
      }

      return Range.range(lower, lowerBound, upper, upperBound);
    } else if (lower != null) {
      return Range.downTo(lower, lowerBound);
    } else if (upper != null) {
      return Range.upTo(upper, upperBound);
    } else {
      return Range.all();
    }
  }

  /**
   * Parse a numeric range from attributes on the given element specifying the bounds of the range,
   * specifically:
   *
   * <p>gt gte lt lte
   */
  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Element el, Class<T> type) throws InvalidXMLException {
    return parseNumericRange(el, type, Range.all());
  }

  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Element el, Class<T> type, Range<T> def) throws InvalidXMLException {
    Attribute count = el.getAttribute("count");

    Attribute lt = el.getAttribute("lt");
    Attribute lte = getAttribute(el, "lte", "max");
    Attribute gt = el.getAttribute("gt");
    Attribute gte = getAttribute(el, "gte", "min");

    if (count != null && (lt != null || lte != null || gt != null || gte != null))
      throw new InvalidXMLException("Count cannot be combined with min or max", el);
    if (lt != null && lte != null)
      throw new InvalidXMLException("Conflicting upper bound for numeric range", el);
    if (gt != null && gte != null)
      throw new InvalidXMLException("Conflicting lower bound for numeric range", el);

    if (count != null) return Range.singleton(parseNumber(count, type, (T) null));

    BoundType lowerBoundType, upperBoundType;
    T lowerBound, upperBound;

    if (gt != null) {
      lowerBound = parseNumber(gt, type, (T) null);
      lowerBoundType = BoundType.OPEN;
    } else {
      lowerBound = parseNumber(gte, type, (T) null);
      lowerBoundType = BoundType.CLOSED;
    }

    if (lt != null) {
      upperBound = parseNumber(lt, type, (T) null);
      upperBoundType = BoundType.OPEN;
    } else {
      upperBound = parseNumber(lte, type, (T) null);
      upperBoundType = BoundType.CLOSED;
    }

    if (lowerBound == null) {
      if (upperBound == null) {
        return def;
      } else {
        return Range.upTo(upperBound, upperBoundType);
      }
    } else {
      if (upperBound == null) {
        return Range.downTo(lowerBound, lowerBoundType);
      } else {
        return Range.range(lowerBound, lowerBoundType, upperBound, upperBoundType);
      }
    }
  }

  public static <T extends Number & Comparable<T>> Range<T> parseBoundedNumericRange(
      Node node, Class<T> type) throws InvalidXMLException {
    Range<T> result = parseNumericRange(node, type);
    if (!Ranges.isBounded(result))
      throw new InvalidXMLException("Range for this node needs to be bounded", node);
    return result;
  }

  public static <T extends Number & Comparable<T>> Range<T> parseBoundedNumericRange(
      Attribute attribute, Class<T> type, Range<T> def) throws InvalidXMLException {
    if (attribute != null && attribute.getValue() != null)
      return parseBoundedNumericRange(new Node(attribute), type);
    return def;
  }

  public static <T extends Number & Comparable<T>> Range<T> parseBoundedNumericRange(
      Attribute attribute, Class<T> type) throws InvalidXMLException {
    return parseBoundedNumericRange(attribute, type, null);
  }

  public static Duration parseDuration(Node node, Duration def) throws InvalidXMLException {
    if (node == null) {
      return def;
    }
    try {
      return TextParser.parseDuration(node.getValueNormalize());
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  @Contract("null -> null; !null -> !null")
  public static Duration parseDuration(Node node) throws InvalidXMLException {
    return parseDuration(node, null);
  }

  public static Duration parseDuration(Element el, Duration def) throws InvalidXMLException {
    return parseDuration(Node.fromNullable(el), def);
  }

  public static Duration parseDuration(Attribute attr, Duration def) throws InvalidXMLException {
    return parseDuration(Node.fromNullable(attr), def);
  }

  public static Duration parseDuration(Attribute attr) throws InvalidXMLException {
    return parseDuration(attr, null);
  }

  public static Duration parseTickDuration(Node node, String text) throws InvalidXMLException {
    if ("oo".equals(text)) return TimeUtils.INFINITE_DURATION;
    try {
      return Duration.ofMillis(Integer.parseInt(text) * 50);
    } catch (NumberFormatException e) {
      return parseDuration(node);
    }
  }

  public static Duration parseSecondDuration(Node node, String text) throws InvalidXMLException {
    try {
      return Duration.ofSeconds(Integer.parseInt(text));
    } catch (NumberFormatException e) {
      return parseDuration(node);
    }
  }

  public static Duration parseSecondDuration(Node node) throws InvalidXMLException {
    return parseSecondDuration(node, node.getValueNormalize());
  }

  public static Duration parseSecondDuration(Node node, Duration def) throws InvalidXMLException {
    return node == null ? def : parseSecondDuration(node);
  }

  public static Class<? extends Entity> parseEntityType(Element el) throws InvalidXMLException {
    return parseEntityType(new Node(el));
  }

  public static Class<? extends Entity> parseEntityTypeAttribute(
      Element el, String attributeName, Class<? extends Entity> def) throws InvalidXMLException {
    Node node = Node.fromAttr(el, attributeName);
    return node == null ? def : parseEntityType(node);
  }

  public static Class<? extends Entity> parseEntityType(Node node) throws InvalidXMLException {
    return parseEntityType(node, node.getValue());
  }

  public static Class<? extends Entity> parseEntityType(Node node, String value)
      throws InvalidXMLException {
    if (!value.matches("[a-zA-Z0-9_]+")) {
      throw new InvalidXMLException("Invalid entity type '" + value + "'", node);
    }

    try {
      return Class.forName("org.bukkit.entity." + value).asSubclass(Entity.class);
    } catch (ClassNotFoundException | ClassCastException e) {
      throw new InvalidXMLException("Invalid entity type '" + value + "'", node);
    }
  }

  public static OffsetVector parseOffsetVector(Node node) throws InvalidXMLException {
    String value = node.getValueNormalize();
    String[] coords = value.split("\\s*,\\s*");
    Vector vector = parseVector(node, value.replaceAll("[\\^~]", ""));

    boolean local = value.startsWith("^");
    boolean[] relative = new boolean[3];
    for (int i = 0; i < coords.length; i++) {
      relative[i] = coords[i].startsWith("~");
      if (coords[i].startsWith("^") != local)
        throw new InvalidXMLException("Cannot mix world & local coordinates", node);
    }
    return OffsetVector.of(vector, relative, local);
  }

  public static Vector parseVector(Node node, String value) throws InvalidXMLException {
    if (node == null) return null;

    String[] components = value.trim().split("\\s*,\\s*");
    if (components.length != 3) {
      throw new InvalidXMLException("Invalid vector format", node);
    }
    try {
      return new Vector(
          parseNumber(node, components[0], Double.class, true),
          parseNumber(node, components[1], Double.class, true),
          parseNumber(node, components[2], Double.class, true));
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid vector format", node);
    }
  }

  public static Vector parseVector(Node node) throws InvalidXMLException {
    return node == null ? null : parseVector(node, node.getValue());
  }

  public static Vector parseVector(Attribute attr, String value) throws InvalidXMLException {
    return attr == null ? null : parseVector(new Node(attr), value);
  }

  public static Vector parseVector(Attribute attr) throws InvalidXMLException {
    return attr == null ? null : parseVector(attr, attr.getValue());
  }

  public static Vector parseVector(Attribute attr, Vector def) throws InvalidXMLException {
    return attr == null ? def : parseVector(attr);
  }

  public static Vector parse2DVector(Node node, String value) throws InvalidXMLException {
    String[] components = value.trim().split("\\s*,\\s*");
    if (components.length != 2) {
      throw new InvalidXMLException("Invalid 2D vector format", node);
    }
    try {
      return new Vector(
          parseNumber(node, components[0], Double.class, true),
          0d,
          parseNumber(node, components[1], Double.class, true));
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid 2D vector format", node);
    }
  }

  public static Vector parse2DVector(Node node) throws InvalidXMLException {
    return parse2DVector(node, node.getValue());
  }

  public static BlockVector parseBlockVector(Node node, BlockVector def)
      throws InvalidXMLException {
    if (node == null) return def;

    return parseVector(node).toBlockVector();
  }

  public static BlockVector parseBlockVector(Node node) throws InvalidXMLException {
    return parseBlockVector(node, null);
  }

  public static DyeColor parseDyeColor(Attribute attr) throws InvalidXMLException {
    DyeColor result = DyeColors.getByName(attr.getValue());
    if (result == null)
      throw new InvalidXMLException("Invalid dye color '" + attr.getValue() + "'", attr);
    return result;
  }

  public static DyeColor parseDyeColor(Attribute attr, DyeColor def) throws InvalidXMLException {
    return attr == null ? def : parseDyeColor(attr);
  }

  public static Material parseMaterial(Node node, String text) throws InvalidXMLException {
    return MATERIAL_UTILS.parseMaterial(text, node);
  }

  public static Material parseMaterial(Node node) throws InvalidXMLException {
    return parseMaterial(node, node.getValueNormalize());
  }

  public static BlockMaterialData parseBlockMaterialData(Node node, String text)
      throws InvalidXMLException {
    if (node == null) return null;
    return MATERIAL_UTILS.parseBlockMaterialData(text, node);
  }

  public static BlockMaterialData parseBlockMaterialData(Node node, BlockMaterialData def)
      throws InvalidXMLException {
    return node == null ? def : parseBlockMaterialData(node, node.getValueNormalize());
  }

  public static BlockMaterialData parseBlockMaterialData(Node node) throws InvalidXMLException {
    return parseBlockMaterialData(node, (BlockMaterialData) null);
  }

  public static ItemMaterialData parseItemMaterialData(Node node, String text)
      throws InvalidXMLException {
    if (node == null) return null;
    return MATERIAL_UTILS.parseItemMaterialData(text, node);
  }

  public static ItemMaterialData parseItemMaterialData(Node node, String text, short dmg)
      throws InvalidXMLException {
    if (node == null) return null;
    return MATERIAL_UTILS.parseItemMaterialData(text, dmg, node);
  }

  public static ItemMaterialData parseItemMaterialData(Node node, ItemMaterialData def)
      throws InvalidXMLException {
    return node == null ? def : parseItemMaterialData(node, node.getValueNormalize());
  }

  public static ItemMaterialData parseItemMaterialData(Node node) throws InvalidXMLException {
    return parseItemMaterialData(node, (ItemMaterialData) null);
  }

  public static MaterialMatcher parseMaterialMatcher(Element el) throws InvalidXMLException {
    MaterialMatcher.Builder builder = MaterialMatcher.builder();

    for (Element elChild : el.getChildren()) {
      switch (elChild.getName()) {
        case "all-materials":
        case "all-items":
          return AllMaterialMatcher.INSTANCE;

        case "all-blocks":
          builder.add(BlockMaterialMatcher.INSTANCE);
          break;

        case "material":
        case "item":
          builder.parse(new Node(elChild));
          break;

        default:
          throw new InvalidXMLException("Unknown material matcher tag", elChild);
      }
    }

    return builder.build();
  }

  public static PotionEffectType parsePotionEffectType(Node node) throws InvalidXMLException {
    return parsePotionEffectType(node, node.getValue());
  }

  public static PotionEffectType parsePotionEffectType(Node node, String text)
      throws InvalidXMLException {
    PotionEffectType type = PotionEffects.getByName(text);
    if (type == null) {
      throw new InvalidXMLException("Unknown potion type '" + node.getValue() + "'", node);
    }
    return type;
  }

  private static PotionEffect createPotionEffect(
      PotionEffectType type, Duration duration, int amplifier, boolean ambient) {
    // Modern supports infinite durations with value -1
    int ticks = Platform.isModern() && TimeUtils.isInfinite(duration)
        ? -1
        : (int) TimeUtils.toTicks(duration);
    return new PotionEffect(type, ticks, amplifier, ambient);
  }

  public static PotionEffect parsePotionEffect(Element el) throws InvalidXMLException {
    return parsePotionEffect(el, false);
  }

  public static PotionEffect parsePotionEffect(Element el, boolean ambientDef)
      throws InvalidXMLException {
    PotionEffectType type = parsePotionEffectType(new Node(el));
    Duration duration =
        parseSecondDuration(Node.fromAttr(el, "duration"), TimeUtils.INFINITE_DURATION);
    int amplifier = parseNumber(Node.fromAttr(el, "amplifier"), Integer.class, 1) - 1;
    boolean ambient = parseBoolean(Node.fromAttr(el, "ambient"), ambientDef);

    return createPotionEffect(type, duration, amplifier, ambient);
  }

  public static PotionEffect parseCompactPotionEffect(Node node, String text, boolean particle)
      throws InvalidXMLException {
    String[] parts = text.split(":");

    if (parts.length == 0) throw new InvalidXMLException("Missing potion effect type", node);
    PotionEffectType type = parsePotionEffectType(node, parts[0]);
    Duration duration = TimeUtils.INFINITE_DURATION;
    int amplifier = 0;
    boolean ambient = !particle;

    if (parts.length >= 2) {
      duration = parseTickDuration(node, parts[1]);
      if (parts.length >= 3) {
        amplifier = parseNumber(node, parts[2], Integer.class);
        if (parts.length >= 4) {
          ambient = parseBoolean(node, parts[3]);
        }
      }
    }

    return createPotionEffect(type, duration, amplifier, ambient);
  }

  public static <T extends Enum<T>> T parseEnum(Node node, String text, Class<T> type)
      throws InvalidXMLException {
    try {
      return TextParser.parseEnum(text, type);
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static <T extends Enum<T>> T parseEnum(@Nullable Node node, Class<T> type, @Nullable T def)
      throws InvalidXMLException {
    if (node == null) return def;
    return parseEnum(node, node.getValueNormalize(), type);
  }

  public static <T extends Enum<T>> T parseEnum(@Nullable Node node, Class<T> type)
      throws InvalidXMLException {
    return parseEnum(node, type, null);
  }

  public static <T extends Enum<T>> T parseEnum(Element el, Class<T> type)
      throws InvalidXMLException {
    return parseEnum(new Node(el), type);
  }

  public static <T extends Enum<T>> T parseEnum(Attribute attr, Class<T> type)
      throws InvalidXMLException {
    return parseEnum(new Node(attr), type);
  }

  public static ChatColor parseChatColor(@Nullable Node node) throws InvalidXMLException {
    return parseEnum(node, ChatColor.class);
  }

  public static ChatColor parseChatColor(@Nullable Node node, ChatColor def)
      throws InvalidXMLException {
    return node == null ? def : parseChatColor(node);
  }

  public static String getNormalizedNullableText(Element el) {
    String text = el.getTextNormalize();
    if (text == null || "".equals(text)) {
      return null;
    } else {
      return text;
    }
  }

  public static String getNullableAttribute(Element el, String... attrs) {
    String text = null;
    for (String attr : attrs) {
      text = el.getAttributeValue(attr);
      if (text != null) break;
    }
    return text;
  }

  public static UUID parseUuid(@Nullable Node node) throws InvalidXMLException {
    if (node == null) return null;
    try {
      return TextParser.parseUuid(node.getValueNormalize());
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

  public static String parseUsername(@Nullable Node node) throws InvalidXMLException {
    if (node == null) return null;
    String name = node.getValueNormalize();
    if (!USERNAME_REGEX.matcher(name).matches()) {
      throw new InvalidXMLException("Invalid Minecraft username '" + name + "'", node);
    }
    return name;
  }

  public static Skin parseUnsignedSkin(@Nullable Node node) throws InvalidXMLException {
    if (node == null) return null;
    String data = node.getValueNormalize();
    try {
      Base64.getDecoder().decode(data.getBytes());
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException("Skin data is not valid base64", node);
    }
    return new Skin(data, null);
  }

  /**
   * Parse a piece of formatted text, which can be either plain text with legacy formatting codes,
   * or JSON chat components.
   */
  public static Component parseFormattedText(@Nullable Node node, Component def)
      throws InvalidXMLException {
    return node == null
        ? def
        : TextParser.parseComponentSection(BukkitUtils.colorize(node.getValueNormalize()));
  }

  /**
   * Parse a piece of formatted text, which can be either plain text with legacy formatting codes,
   * or JSON chat components.
   */
  public static @Nullable Component parseFormattedText(@Nullable Node node)
      throws InvalidXMLException {
    return parseFormattedText(node, null);
  }

  /**
   * Parse a piece of formatted text, which can be either plain text with legacy formatting codes,
   * or JSON chat components.
   */
  public static Component parseFormattedText(Element parent, String property, Component def)
      throws InvalidXMLException {
    return parseFormattedText(Node.fromChildOrAttr(parent, property), def);
  }

  /**
   * Parse a piece of formatted text, which can be either plain text with legacy formatting codes,
   * or JSON chat components.
   */
  public static Component parseFormattedText(Element parent, String property)
      throws InvalidXMLException {
    return parseFormattedText(Node.fromChildOrAttr(parent, property));
  }

  public static NameTagVisibility parseNameTagVisibility(Node node, NameTagVisibility def)
      throws InvalidXMLException {
    if (node == null) return def;

    switch (node.getValue()) {
      case "yes":
      case "on":
      case "true":
        return NameTagVisibility.ALWAYS;

      case "no":
      case "off":
      case "false":
        return NameTagVisibility.NEVER;

      case "ally":
      case "allies":
        return NameTagVisibility.HIDE_FOR_OTHER_TEAMS;

      case "enemy":
      case "enemies":
        return NameTagVisibility.HIDE_FOR_OWN_TEAM;

      default:
        throw new InvalidXMLException("Invalid name tag visibility value", node);
    }
  }

  public static Enchantment parseEnchantment(Node node) throws InvalidXMLException {
    return parseEnchantment(node, node.getValueNormalize());
  }

  public static Enchantment parseEnchantment(Node node, String text) throws InvalidXMLException {
    Enchantment enchantment = Enchantments.getByName(text);
    if (enchantment == null) {
      throw new InvalidXMLException("Unknown enchantment '" + text + "'", node);
    }

    return enchantment;
  }

  public static org.bukkit.attribute.Attribute parseAttribute(Node node, String text)
      throws InvalidXMLException {
    var attribute = Attributes.getByName(text);
    if (attribute != null) return attribute;

    attribute = Attributes.getByName("generic" + text);
    if (attribute != null) return attribute;

    throw new InvalidXMLException("Unknown attribute '" + text + "'", node);
  }

  public static org.bukkit.attribute.Attribute parseAttribute(Node node)
      throws InvalidXMLException {
    return parseAttribute(node, node.getValueNormalize());
  }

  public static AttributeModifier.Operation parseAttributeOperation(Node node, String text)
      throws InvalidXMLException {
    return switch (text.toLowerCase(Locale.ROOT)) {
      case "add" -> AttributeModifier.Operation.ADD_NUMBER;
      case "base" -> AttributeModifier.Operation.ADD_SCALAR;
      case "multiply" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
      default -> throw new InvalidXMLException(
          "Unknown attribute modifier operation '" + text + "'", node);
    };
  }

  public static AttributeModifier.Operation parseAttributeOperation(Node node)
      throws InvalidXMLException {
    return node == null
        ? AttributeModifier.Operation.ADD_NUMBER
        : parseAttributeOperation(node, node.getValueNormalize());
  }

  public static Pair<org.bukkit.attribute.Attribute, AttributeModifier>
      parseCompactAttributeModifier(Node node, String text) throws InvalidXMLException {
    String[] parts = text.split(":");
    if (parts.length != 3) throw new InvalidXMLException("Bad attribute modifier format", node);

    return Pair.of(
        parseAttribute(node, parts[0]),
        new AttributeModifier(
            "FromXML",
            parseNumber(node, parts[2], Double.class),
            parseAttributeOperation(node, parts[1])));
  }

  public static Pair<org.bukkit.attribute.Attribute, AttributeModifier> parseAttributeModifier(
      Element el) throws InvalidXMLException {
    return new Pair<>(parseAttribute(new Node(el)), ATTRIBUTE_UTILS.parseModifier(el));
  }

  public static GameMode parseGameMode(Node node, String text) throws InvalidXMLException {
    try {
      return TextParser.parseEnum(text, GameMode.class);
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static GameMode parseGameMode(Node node) throws InvalidXMLException {
    return parseGameMode(node, node.getValueNormalize());
  }

  public static GameMode parseGameMode(Node node, GameMode def) throws InvalidXMLException {
    return node == null ? def : parseGameMode(node);
  }

  public static Version parseSemanticVersion(Node node) throws InvalidXMLException {
    if (node == null) return null;

    try {
      return TextParser.parseVersion(node.getValueNormalize());
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static LocalDate parseDate(Node node) throws InvalidXMLException {
    if (node == null) return null;

    try {
      return TextParser.parseDate(node.getValueNormalize());
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static Title.Times parseTitleTimes(Element el, Title.Times def)
      throws InvalidXMLException {
    Duration fadeIn = XMLUtils.parseDuration(Node.fromAttr(el, "fade-in"), def.fadeIn());
    Duration stay = XMLUtils.parseDuration(Node.fromAttr(el, "stay"), def.stay());
    Duration fadeOut = XMLUtils.parseDuration(Node.fromAttr(el, "fade-out"), def.fadeOut());

    return Title.Times.times(fadeIn, stay, fadeOut);
  }

  public static Color parseHexColor(Node node) throws InvalidXMLException {
    if (node == null || node.getValue() == null)
      throw new InvalidXMLException("No value provided for color", node);
    String rawColor = node.getValue();
    if (!rawColor.matches("[a-fA-F0-9]{6}")) {
      throw new InvalidXMLException("Invalid color format '" + rawColor + "'", node);
    }
    return Color.fromRGB(Integer.parseInt(rawColor, 16));
  }
}
