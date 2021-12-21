package tc.oc.pgm.util.xml;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.material.matcher.AllMaterialMatcher;
import tc.oc.pgm.util.material.matcher.BlockMaterialMatcher;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.nms.NMSHacks;
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
    for (Element child : root.getChildren()) {
      if (parentTagNames.contains(child.getName())) {
        result.addAll(
            flattenElements(
                new InheritingElement(child), parentTagNames, childTagNames, minChildDepth - 1));
      } else if (minChildDepth <= 0
          && (childTagNames == null || childTagNames.contains(child.getName()))) {
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
    return Iterables.filter(
        parent.getChildren(),
        new Predicate<Element>() {
          @Override
          public boolean apply(Element child) {
            return nameSet.contains(child.getName());
          }
        });
  }

  public static Iterable<Attribute> getAttributes(Element parent, String... names) {
    final Set<String> nameSet = new HashSet<>(Arrays.asList(names));
    return Iterables.filter(
        parent.getAttributes(),
        new Predicate<Attribute>() {
          @Override
          public boolean apply(Attribute child) {
            return nameSet.contains(child.getName());
          }
        });
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

  /**
   * Parse a range in the standard mathematical format e.g.
   *
   * <p>[0, 1)
   *
   * <p>for a closed-open range from 0 to 1
   *
   * <p>Also supports singleton ranges derived from providing a number with no delimiter
   */
  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Node node, Class<T> type) throws InvalidXMLException {
    Matcher matcher = RANGE_RE.matcher(node.getValue());
    if (!matcher.matches()) {
      T value = parseNumber(node, node.getValue(), type, true);
      if (value != null) {
        return Range.singleton(value);
      }
      throw new InvalidXMLException(
          "Invalid " + type.getSimpleName().toLowerCase() + " range '" + node.getValue() + "'",
          node);
    }

    T lower = parseNumber(node, matcher.group(2), type, true);
    T upper = parseNumber(node, matcher.group(3), type, true);

    BoundType lowerType = null, upperType = null;
    if (!Double.isInfinite(lower.doubleValue())) {
      lowerType = "(".equals(matcher.group(1)) ? BoundType.OPEN : BoundType.CLOSED;
    }
    if (!Double.isInfinite(upper.doubleValue())) {
      upperType = ")".equals(matcher.group(4)) ? BoundType.OPEN : BoundType.CLOSED;
    }

    if (lower.compareTo(upper) == 1) {
      throw new InvalidXMLException(
          "range lower bound (" + lower + ") cannot be greater than upper bound (" + upper + ")",
          node);
    }

    if (lowerType == null) {
      if (upperType == null) {
        return Range.all();
      } else {
        return Range.upTo(upper, upperType);
      }
    } else {
      if (upperType == null) {
        return Range.downTo(lower, lowerType);
      } else {
        return Range.range(lower, lowerType, upper, upperType);
      }
    }
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

  public static @Nullable Duration parseDuration(Node node) throws InvalidXMLException {
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

    String[] components = node.getValue().trim().split("\\s*,\\s*");
    if (components.length != 3) {
      throw new InvalidXMLException("Invalid block location", node);
    }
    try {
      return new BlockVector(
          Integer.parseInt(components[0]),
          Integer.parseInt(components[1]),
          Integer.parseInt(components[2]));
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid block location", node);
    }
  }

  public static BlockVector parseBlockVector(Node node) throws InvalidXMLException {
    return parseBlockVector(node, null);
  }

  public static DyeColor parseDyeColor(Attribute attr) throws InvalidXMLException {
    String name = attr.getValue().replace(" ", "_").toUpperCase();
    try {
      return DyeColor.valueOf(name);
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException("Invalid dye color '" + attr.getValue() + "'", attr);
    }
  }

  public static DyeColor parseDyeColor(Attribute attr, DyeColor def) throws InvalidXMLException {
    return attr == null ? def : parseDyeColor(attr);
  }

  public static Material parseMaterial(Node node, String text) throws InvalidXMLException {
    Material material = Materials.parseMaterial(text);
    if (material == null) {
      throw new InvalidXMLException("Unknown material '" + text + "'", node);
    }
    return material;
  }

  public static Material parseMaterial(Node node) throws InvalidXMLException {
    return parseMaterial(node, node.getValueNormalize());
  }

  public static MaterialData parseMaterialData(Node node, String text) throws InvalidXMLException {
    String[] pieces = text.split(":");
    Material material = parseMaterial(node, pieces[0]);
    byte data;
    if (pieces.length > 1) {
      data = parseNumber(node, pieces[1], Byte.class);
    } else {
      data = 0;
    }
    return material.getNewData(data);
  }

  public static MaterialData parseMaterialData(Node node, MaterialData def)
      throws InvalidXMLException {
    return node == null ? def : parseMaterialData(node, node.getValueNormalize());
  }

  public static MaterialData parseMaterialData(Node node) throws InvalidXMLException {
    return parseMaterialData(node, (MaterialData) null);
  }

  public static MaterialData parseBlockMaterialData(Node node, String text)
      throws InvalidXMLException {
    if (node == null) return null;
    MaterialData material = parseMaterialData(node, text);
    if (!material.getItemType().isBlock()) {
      throw new InvalidXMLException(
          "Material " + material.getItemType().name() + " is not a block", node);
    }
    return material;
  }

  public static MaterialData parseBlockMaterialData(Node node, MaterialData def)
      throws InvalidXMLException {
    return node == null ? def : parseBlockMaterialData(node, node.getValueNormalize());
  }

  public static MaterialData parseBlockMaterialData(Node node) throws InvalidXMLException {
    return parseBlockMaterialData(node, (MaterialData) null);
  }

  public static SingleMaterialMatcher parseMaterialPattern(Node node, String value)
      throws InvalidXMLException {
    try {
      return SingleMaterialMatcher.parse(value);
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException(e.getMessage(), node);
    }
  }

  public static SingleMaterialMatcher parseMaterialPattern(Node node) throws InvalidXMLException {
    return parseMaterialPattern(node, node.getValue());
  }

  public static SingleMaterialMatcher parseMaterialPattern(Node node, SingleMaterialMatcher def)
      throws InvalidXMLException {
    return node == null ? def : parseMaterialPattern(node);
  }

  public static SingleMaterialMatcher parseMaterialPattern(Element el) throws InvalidXMLException {
    return parseMaterialPattern(new Node(el));
  }

  public static SingleMaterialMatcher parseMaterialPattern(Attribute attr)
      throws InvalidXMLException {
    return parseMaterialPattern(new Node(attr));
  }

  public static ImmutableSet<SingleMaterialMatcher> parseMaterialPatternSet(Node node)
      throws InvalidXMLException {
    ImmutableSet.Builder<SingleMaterialMatcher> patterns = ImmutableSet.builder();
    for (String value : Splitter.on(";").split(node.getValue())) {
      patterns.add(parseMaterialPattern(node, value));
    }
    return patterns.build();
  }

  public static MaterialMatcher parseMaterialMatcher(Element el) throws InvalidXMLException {
    Set<MaterialMatcher> matchers = new HashSet<>();

    for (Element elChild : el.getChildren()) {
      switch (elChild.getName()) {
        case "all-materials":
        case "all-items":
          return AllMaterialMatcher.INSTANCE;

        case "all-blocks":
          matchers.add(BlockMaterialMatcher.INSTANCE);
          break;

        case "material":
        case "item":
          matchers.add(parseMaterialPattern(elChild));
          break;

        default:
          throw new InvalidXMLException("Unknown material matcher tag", elChild);
      }
    }

    return CompoundMaterialMatcher.of(matchers);
  }

  public static PotionEffectType parsePotionEffectType(Node node) throws InvalidXMLException {
    return parsePotionEffectType(node, node.getValue());
  }

  public static PotionEffectType parsePotionEffectType(Node node, String text)
      throws InvalidXMLException {
    PotionEffectType type = PotionEffectType.getByName(text.toUpperCase().replace(" ", "_"));
    if (type == null) type = NMSHacks.getPotionEffectType(text);
    if (type == null) {
      throw new InvalidXMLException("Unknown potion type '" + node.getValue() + "'", node);
    }
    return type;
  }

  private static PotionEffect createPotionEffect(
      PotionEffectType type, Duration duration, int amplifier, boolean ambient) {
    return new PotionEffect(type, (int) TimeUtils.toTicks(duration), amplifier, ambient);
  }

  public static PotionEffect parsePotionEffect(Element el) throws InvalidXMLException {
    PotionEffectType type = parsePotionEffectType(new Node(el));
    Duration duration =
        parseSecondDuration(Node.fromAttr(el, "duration"), TimeUtils.INFINITE_DURATION);
    int amplifier = parseNumber(Node.fromAttr(el, "amplifier"), Integer.class, 1) - 1;
    boolean ambient = parseBoolean(Node.fromAttr(el, "ambient"), false);

    return createPotionEffect(type, duration, amplifier, ambient);
  }

  public static PotionEffect parseCompactPotionEffect(Node node, String text)
      throws InvalidXMLException {
    String[] parts = text.split(":");

    if (parts.length == 0) throw new InvalidXMLException("Missing potion effect type", node);
    PotionEffectType type = parsePotionEffectType(node, parts[0]);
    Duration duration = TimeUtils.INFINITE_DURATION;
    int amplifier = 0;
    boolean ambient = false;

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

  public static <T extends Enum<T>> T parseEnum(
      Node node, String text, Class<T> type, String readableType) throws InvalidXMLException {
    try {
      return TextParser.parseEnum(text, type);
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  public static <T extends Enum<T>> T parseEnum(
      @Nullable Node node, Class<T> type, String readableType, @Nullable T def)
      throws InvalidXMLException {
    if (node == null) return def;
    return parseEnum(node, node.getValueNormalize(), type, readableType);
  }

  public static <T extends Enum<T>> T parseEnum(
      @Nullable Node node, Class<T> type, String readableType) throws InvalidXMLException {
    return parseEnum(node, type, readableType, null);
  }

  public static <T extends Enum<T>> T parseEnum(Element el, Class<T> type)
      throws InvalidXMLException {
    return parseEnum(new Node(el), type, type.getSimpleName());
  }

  public static <T extends Enum<T>> T parseEnum(Element el, Class<T> type, String readableType)
      throws InvalidXMLException {
    return parseEnum(new Node(el), type, readableType);
  }

  public static <T extends Enum<T>> T parseEnum(Attribute attr, Class<T> type, String readableType)
      throws InvalidXMLException {
    return parseEnum(new Node(attr), type, readableType);
  }

  public static ChatColor parseChatColor(@Nullable Node node) throws InvalidXMLException {
    return parseEnum(node, ChatColor.class, "color");
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
    Enchantment enchantment = Enchantment.getByName(text.toUpperCase().replace(" ", "_"));
    if (enchantment == null) enchantment = NMSHacks.getEnchantment(text);

    if (enchantment == null) {
      throw new InvalidXMLException("Unknown enchantment '" + text + "'", node);
    }

    return enchantment;
  }

  public static tc.oc.pgm.util.attribute.Attribute parseAttribute(Node node, String text)
      throws InvalidXMLException {
    tc.oc.pgm.util.attribute.Attribute attribute = tc.oc.pgm.util.attribute.Attribute.byName(text);
    if (attribute != null) return attribute;

    attribute = tc.oc.pgm.util.attribute.Attribute.byName("generic." + text);
    if (attribute != null) return attribute;

    throw new InvalidXMLException("Unknown attribute '" + text + "'", node);
  }

  public static tc.oc.pgm.util.attribute.Attribute parseAttribute(Node node)
      throws InvalidXMLException {
    return parseAttribute(node, node.getValueNormalize());
  }

  public static AttributeModifier.Operation parseAttributeOperation(Node node, String text)
      throws InvalidXMLException {
    switch (text.toLowerCase()) {
      case "add":
        return AttributeModifier.Operation.ADD_NUMBER;
      case "base":
        return AttributeModifier.Operation.ADD_SCALAR;
      case "multiply":
        return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
    }
    throw new InvalidXMLException("Unknown attribute modifier operation '" + text + "'", node);
  }

  public static AttributeModifier.Operation parseAttributeOperation(Node node)
      throws InvalidXMLException {
    return parseAttributeOperation(node, node.getValueNormalize());
  }

  public static AttributeModifier.Operation parseAttributeOperation(
      Node node, AttributeModifier.Operation def) throws InvalidXMLException {
    return node == null ? def : parseAttributeOperation(node);
  }

  public static Map.Entry<String, AttributeModifier> parseCompactAttributeModifier(
      Node node, String text) throws InvalidXMLException {
    String[] parts = text.split(":");

    if (parts.length != 3) {
      throw new InvalidXMLException("Bad attribute modifier format", node);
    }

    tc.oc.pgm.util.attribute.Attribute attribute = parseAttribute(node, parts[0]);
    AttributeModifier.Operation operation = parseAttributeOperation(node, parts[1]);
    double amount = parseNumber(node, parts[2], Double.class);

    return new AbstractMap.SimpleImmutableEntry<>(
        attribute.getName(), new AttributeModifier("FromXML", amount, operation));
  }

  public static Map.Entry<String, AttributeModifier> parseAttributeModifier(Element el)
      throws InvalidXMLException {
    String attribute = parseAttribute(new Node(el)).getName();
    double amount = parseNumber(Node.fromRequiredAttr(el, "amount"), Double.class);
    AttributeModifier.Operation operation =
        parseAttributeOperation(
            Node.fromAttr(el, "operation"), AttributeModifier.Operation.ADD_NUMBER);

    return new AbstractMap.SimpleImmutableEntry<>(
        attribute, new AttributeModifier("FromXML", amount, operation));
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
}
