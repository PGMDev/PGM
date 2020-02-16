package tc.oc.pgm.util;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
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
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.material.MaterialMatcher;
import tc.oc.material.matcher.AllMaterialMatcher;
import tc.oc.material.matcher.BlockMaterialMatcher;
import tc.oc.material.matcher.CompoundMaterialMatcher;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.server.BukkitUtils;
import tc.oc.util.Numbers;
import tc.oc.util.Pair;
import tc.oc.util.TimeUtils;
import tc.oc.util.Version;
import tc.oc.util.collection.ArrayUtils;
import tc.oc.util.components.Components;
import tc.oc.util.components.PeriodFormats;
import tc.oc.world.NMSHacks;
import tc.oc.xml.InheritingElement;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

// TODO: move to tc.oc.xml
public class XMLUtils {
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

  public static Element getUniqueChild(Element parent, String name, String... aliases)
      throws InvalidXMLException {
    aliases = ArrayUtils.append(aliases, name);

    List<Element> children = new ArrayList<>();
    for (String alias : aliases) {
      children.addAll(parent.getChildren(alias));
    }

    if (children.size() > 1) {
      throw new InvalidXMLException("multiple '" + aliases[0] + "' tags not allowed", parent);
    }
    return children.isEmpty() ? null : children.get(0);
  }

  public static Element getRequiredUniqueChild(Element parent, String name, String... aliases)
      throws InvalidXMLException {
    aliases = ArrayUtils.append(aliases, name);

    List<Element> children = new ArrayList<>();
    for (String alias : aliases) {
      children.addAll(parent.getChildren(alias));
    }

    if (children.size() > 1) {
      throw new InvalidXMLException("multiple '" + name + "' tags not allowed", parent);
    } else if (children.isEmpty()) {
      throw new InvalidXMLException("child tag '" + name + "' is required", parent);
    }
    return children.get(0);
  }

  public static Attribute getRequiredAttribute(Element el, String name, String... aliases)
      throws InvalidXMLException {
    aliases = ArrayUtils.append(aliases, name);

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
      throw new InvalidXMLException("attribute '" + name + "' is required", el);
    }

    return attr;
  }

  private static Boolean parseBoolean(Node node, String value) throws InvalidXMLException {
    if ("true".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value)
        || "on".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)
        || "no".equalsIgnoreCase(value)
        || "off".equalsIgnoreCase(value)) {
      return false;
    }
    throw new InvalidXMLException("invalid boolean value", node);
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

  public static <T extends Number> T parseNumber(
      Node node, String text, Class<T> type, boolean infinity) throws InvalidXMLException {
    try {
      return Numbers.parse(text, type, infinity);
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
    T value = parseNumber(el, type);
    if (!range.contains(value)) {
      throw new InvalidXMLException(value + " is not in the range " + range, el);
    }
    return value;
  }

  public static <T extends Number & Comparable<T>> T parseNumber(
      Attribute attr, Class<T> type, Range<T> range) throws InvalidXMLException {
    T value = parseNumber(attr, type);
    if (!range.contains(value)) {
      throw new InvalidXMLException(value + " is not in the range " + range, attr);
    }
    return value;
  }

  public static <T extends Number & Comparable<T>> T parseNumber(
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
   */
  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Node node, Class<T> type) throws InvalidXMLException {
    Matcher matcher = RANGE_RE.matcher(node.getValue());
    if (!matcher.matches()) {
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

  /**
   * Parse a numeric range from attributes on the given element specifying the bounds of the range,
   * specifically:
   *
   * <p>gt gte lt lte
   */
  public static <T extends Number & Comparable<T>> Range<T> parseNumericRange(
      Element el, Class<T> type) throws InvalidXMLException {
    Attribute lt = el.getAttribute("lt");
    Attribute lte = el.getAttribute("lte");
    Attribute gt = el.getAttribute("gt");
    Attribute gte = el.getAttribute("gte");

    if (lt != null && lte != null)
      throw new InvalidXMLException("Conflicting upper bound for numeric range", el);
    if (gt != null && gte != null)
      throw new InvalidXMLException("Conflicting lower bound for numeric range", el);

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
        return Range.all();
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

  public static Duration parseDuration(Node node, Duration def) throws InvalidXMLException {
    if (node == null) {
      return def;
    }
    try {
      String value = node.getValueNormalize();
      if ("oo".equals(value)) {
        return TimeUtils.INFINITE_DURATION;
      } else {
        return PeriodFormats.SHORTHAND.parsePeriod(node.getValue()).toStandardDuration();
      }
    } catch (IllegalArgumentException | UnsupportedOperationException e) {
      throw new InvalidXMLException("invalid time format", node, e);
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
      return Duration.millis(Integer.parseInt(text) * 50);
    } catch (NumberFormatException e) {
      return parseDuration(node);
    }
  }

  public static Duration parseTickDuration(Node node) throws InvalidXMLException {
    return parseTickDuration(node, node.getValueNormalize());
  }

  public static Duration parseTickDuration(Node node, Duration def) throws InvalidXMLException {
    return node == null ? def : parseDuration(node);
  }

  public static Duration parseSecondDuration(Node node, String text) throws InvalidXMLException {
    if ("oo".equals(text)) return TimeUtils.INFINITE_DURATION;
    try {
      return Duration.standardSeconds(Integer.parseInt(text));
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

  public static Vector parseVector(Node node, Vector def) throws InvalidXMLException {
    return node == null ? def : parseVector(node);
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

  public static Vector parse2DVector(Node node, Vector def) throws InvalidXMLException {
    return node == null ? def : parse2DVector(node);
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
    Material material = Material.matchMaterial(text);
    if (material == null) {
      throw new InvalidXMLException("Unknown material '" + text + "'", node);
    }
    return material;
  }

  public static Material parseMaterial(Node node) throws InvalidXMLException {
    return parseMaterial(node, node.getValueNormalize());
  }

  public static Material parseBlockMaterial(Node node, String text) throws InvalidXMLException {
    Material material = parseMaterial(node, text);
    if (!material.isBlock()) {
      throw new InvalidXMLException("Material " + material.name() + " is not a block", node);
    }
    return material;
  }

  public static Material parseBlockMaterial(Node node) throws InvalidXMLException {
    return node == null ? null : parseBlockMaterial(node, node.getValueNormalize());
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
    return new PotionEffect(
        type,
        duration == TimeUtils.INFINITE_DURATION
            ? Integer.MAX_VALUE
            : (int) (duration.getMillis() / 50),
        amplifier,
        ambient);
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
      return Enum.valueOf(type, text.trim().toUpperCase().replace(' ', '_'));
    } catch (IllegalArgumentException ex) {
      throw new InvalidXMLException("Unknown " + readableType + " '" + text + "'", node);
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
    String raw = node.getValue();
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException("Invalid UUID format (must be 8-4-4-4-12)", node, e);
    }
  }

  private static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

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
      Base64.decodeBase64(data.getBytes());
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException("Skin data is not valid base64", node);
    }
    return new Skin(data, null);
  }

  /** Guess if the given text is a JSON object by looking for the curly braces at either finish */
  public static boolean looksLikeJson(String text) {
    text = text.trim();
    return text.startsWith("{") && text.endsWith("}");
  }

  /**
   * Parse a piece of formatted text, which can be either plain text with legacy formatting codes,
   * or JSON chat components.
   */
  public static Component parseFormattedText(@Nullable Node node, Component def)
      throws InvalidXMLException {
    if (node == null) return def;

    String text = node.getValueNormalize();
    if (looksLikeJson(text)) {
      try {
        return Components.concat(
            Components.fromBungee(ComponentSerializer.parse(node.getValue()))
                .toArray(new Component[0]));
      } catch (JsonParseException e) {
        throw new InvalidXMLException(e.getMessage(), node, e);
      }
    } else {
      return Components.concat(
          Components.fromBungee(TextComponent.fromLegacyText(BukkitUtils.colorize(text)))
              .toArray(new Component[0]));
    }
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

  public static org.bukkit.attribute.Attribute parseAttribute(Node node, String text)
      throws InvalidXMLException {
    org.bukkit.attribute.Attribute attribute = org.bukkit.attribute.Attribute.byName(text);
    if (attribute != null) return attribute;

    attribute = org.bukkit.attribute.Attribute.byName("generic." + text);
    if (attribute != null) return attribute;

    throw new InvalidXMLException("Unknown attribute '" + text + "'", node);
  }

  public static org.bukkit.attribute.Attribute parseAttribute(Node node)
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

  public static Pair<String, AttributeModifier> parseCompactAttributeModifier(
      Node node, String text) throws InvalidXMLException {
    String[] parts = text.split(":");

    if (parts.length != 3) {
      throw new InvalidXMLException("Bad attribute modifier format", node);
    }

    org.bukkit.attribute.Attribute attribute = parseAttribute(node, parts[0]);
    AttributeModifier.Operation operation = parseAttributeOperation(node, parts[1]);
    double amount = parseNumber(node, parts[2], Double.class);

    return Pair.create(attribute.getName(), new AttributeModifier("FromXML", amount, operation));
  }

  public static Pair<String, AttributeModifier> parseAttributeModifier(Element el)
      throws InvalidXMLException {
    String attribute = parseAttribute(new Node(el)).getName();
    double amount = parseNumber(Node.fromRequiredAttr(el, "amount"), Double.class);
    AttributeModifier.Operation operation =
        parseAttributeOperation(
            Node.fromAttr(el, "operation"), AttributeModifier.Operation.ADD_NUMBER);

    return Pair.create(attribute, new AttributeModifier("FromXML", amount, operation));
  }

  public static GameMode parseGameMode(Node node, String text) throws InvalidXMLException {
    text = text.trim();
    try {
      return GameMode.valueOf(text.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException("Unknown game-mode '" + text + "'", node);
    }
  }

  public static GameMode parseGameMode(Node node) throws InvalidXMLException {
    return parseGameMode(node, node.getValueNormalize());
  }

  public static GameMode parseGameMode(Node node, GameMode def) throws InvalidXMLException {
    return node == null ? def : parseGameMode(node);
  }

  public static Path parseRelativePath(Node node) throws InvalidXMLException {
    return parseRelativePath(node, null);
  }

  public static Path parseRelativePath(Node node, Path def) throws InvalidXMLException {
    if (node == null) return def;
    final String text = node.getValueNormalize();
    try {
      Path path = Paths.get(text);
      if (path.isAbsolute()) {
        throw new InvalidPathException(text, "Path is not relative");
      }
      for (Path part : path) {
        if (part.toString().trim().startsWith("src/test")) {
          throw new InvalidPathException(text, "Path contains an invalid component");
        }
      }
      return path;
    } catch (InvalidPathException e) {
      throw new InvalidXMLException("Invalid relative path '" + text + "'", node, e);
    }
  }

  public static File parseRelativePath(File basePath, Node node, File def)
      throws InvalidXMLException {
    if (node == null) return def;

    File path;
    try {
      path = new File(basePath, node.getValue()).getCanonicalFile();
    } catch (IOException e) {
      throw new InvalidXMLException("Error resolving relative file path", node);
    }

    if (!path.toString().startsWith(basePath.toString())) {
      throw new InvalidXMLException("Invalid relative file path", node);
    }

    return path;
  }

  public static File parseRelativeFolder(File basePath, Node node, File def)
      throws InvalidXMLException {
    File path = parseRelativePath(basePath, node, def);
    if (path != def && !path.isDirectory()) {
      throw new InvalidXMLException("Folder does not exist", node);
    }
    return path;
  }

  public static Version parseSemanticVersion(Node node) throws InvalidXMLException {
    if (node == null) return null;

    String[] parts = node.getValueNormalize().split("\\.", 3);
    if (parts.length < 1 || parts.length > 3) {
      throw new InvalidXMLException(
          "Version must be 1 to 3 whole numbers, separated by periods", node);
    }

    int major = parseNumber(node, parts[0], Integer.class);
    int minor = parts.length < 2 ? 0 : parseNumber(node, parts[1], Integer.class);
    int patch = parts.length < 3 ? 0 : parseNumber(node, parts[2], Integer.class);

    return new Version(major, minor, patch);
  }
}
