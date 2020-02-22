package tc.oc.pgm.regions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureValidation;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.xml.InvalidXMLException;
import tc.oc.util.xml.Node;

public abstract class RegionParser {

  protected final Map<String, Method> methodParsers;
  protected final MapFactory factory;

  public RegionParser(MapFactory factory) {
    this.factory = factory;
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  /**
   * Parse a single region element and return it. Also, store the region in whatever type of context
   * is in use.
   */
  public abstract Region parse(Element el) throws InvalidXMLException;

  public abstract Region parseReference(Attribute attr) throws InvalidXMLException;

  public List<Element> getRegionChildren(Element parent) {
    List<Element> elements = new ArrayList<Element>();
    for (Element el : parent.getChildren()) {
      if (this.isRegion(el)) {
        elements.add(el);
      }
    }
    return elements;
  }

  public Region parseChild(Element parent) throws InvalidXMLException {
    if (parent.getChildren().isEmpty()) {
      throw new InvalidXMLException("Expected a child region", parent);
    } else if (parent.getChildren().size() > 1) {
      throw new InvalidXMLException("Expected only one child region, not multiple", parent);
    }
    return this.parse(parent.getChildren().get(0));
  }

  /**
   * Parse the given element as a wrapper for a single child region, which will be the union of the
   * region referenced by the region="..." attribute (if any) and all nested regions.
   */
  public Region parseChildren(Element parent) throws InvalidXMLException {
    Attribute attrRegion = parent.getAttribute("region");
    Region reference = attrRegion == null ? null : this.parseReference(attrRegion);
    List<Region> regions = this.parseSubRegions(parent);

    if (reference != null) regions.add(reference);

    return Union.of(regions.toArray(new Region[0]));
  }

  public Region[] parseSubRegionsArray(Element parent) throws InvalidXMLException {
    return this.parseSubRegions(parent).toArray(new Region[0]);
  }

  public List<Region> parseSubRegions(Element parent) throws InvalidXMLException {
    List<Region> regions = new ArrayList<>();
    for (Element el : this.getRegionChildren(parent)) {
      regions.add(this.parse(el));
    }
    return regions;
  }

  public @Nullable Region parseRegionProperty(Element rootElement, String... names)
      throws InvalidXMLException {
    return parseRegionProperty(rootElement, null, null, names);
  }

  public @Nullable Region parseRegionProperty(Element rootElement, Region def, String... names)
      throws InvalidXMLException {
    return parseRegionProperty(rootElement, null, def, names);
  }

  public @Nullable Region parseRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      String... names)
      throws InvalidXMLException {
    return parseRegionProperty(rootElement, validation, null, names);
  }

  public @Nullable Region parseRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      Region def,
      String... names)
      throws InvalidXMLException {
    Attribute propertyAttribute = null;
    Element propertyElement = null;

    for (String name : names) {
      if (rootElement.getAttribute(name) != null && rootElement.getChild(name) != null) {
        throw new InvalidXMLException(
            "Multiple defined region properties for " + name, rootElement);
      }

      if ((rootElement.getAttribute(name) != null || rootElement.getChild(name) != null)
          && (propertyAttribute != null || propertyElement != null)) {
        throw new InvalidXMLException(
            "Multiple defined region properties for " + Arrays.toString(names), rootElement);
      }

      if (rootElement.getAttribute(name) != null) {
        propertyAttribute = rootElement.getAttribute(name);
      } else if (rootElement.getChild(name) != null) {
        propertyElement = rootElement.getChild(name);
      }
    }

    Region region = def;
    Node node = null;

    if (propertyAttribute != null) {
      region = this.parseReference(propertyAttribute);
      node = new Node(propertyAttribute);
    } else if (propertyElement != null) {
      region = this.parseChildren(propertyElement);
      node = new Node(propertyElement);
    }

    if (region != null && validation != null) {
      validate(region, validation, node);
    }

    return region;
  }

  public Region parseRequiredRegionProperty(Element rootElement, String... names)
      throws InvalidXMLException {
    return parseRequiredRegionProperty(rootElement, null, names);
  }

  public Region parseRequiredRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      String... names)
      throws InvalidXMLException {
    Region region = parseRegionProperty(rootElement, validation, names);

    if (region == null) {
      throw new InvalidXMLException(
          "No region defined for " + Arrays.toString(names) + " names(s).", rootElement);
    } else {
      return region;
    }
  }

  public void validate(Region region, FeatureValidation<RegionDefinition> validation, Node node)
      throws InvalidXMLException {
    validation.validate((RegionDefinition) region, node);
  }

  protected Method getMethodParser(String regionName) {
    return methodParsers.get(regionName);
  }

  public boolean isRegion(Element el) {
    return methodParsers.containsKey(el.getName());
  }

  protected Region parseDynamic(Element el) throws InvalidXMLException {
    Method parser = this.getMethodParser(el.getName());
    try {
      return (Region) parser.invoke(this, el);
    } catch (Exception e) {
      if (e.getCause() instanceof InvalidXMLException) {
        throw (InvalidXMLException) e.getCause();
      } else {
        throw new InvalidXMLException("Unknown error parsing region: " + e.getMessage(), el, e);
      }
    }
  }

  @MethodParser("half")
  public HalfspaceRegion parseHalfspace(Element el) throws InvalidXMLException {
    Vector normal = XMLUtils.parseVector(XMLUtils.getRequiredAttribute(el, "normal"));
    if (normal.lengthSquared() == 0) {
      throw new InvalidXMLException("normal must have a non-zero length", el);
    }

    Vector origin = XMLUtils.parseVector(el.getAttribute("origin"), new Vector());

    return new HalfspaceRegion(origin, normal);
  }

  protected RegionDefinition parseHalves(Element el, double dir) throws InvalidXMLException {
    Double x = XMLUtils.parseNumber(el.getAttribute("x"), Double.class, (Double) null);
    Double y = XMLUtils.parseNumber(el.getAttribute("y"), Double.class, (Double) null);
    Double z = XMLUtils.parseNumber(el.getAttribute("z"), Double.class, (Double) null);

    List<HalfspaceRegion> halves = new ArrayList<>();
    if (x != null) halves.add(new HalfspaceRegion(new Vector(x, 0, 0), new Vector(dir, 0, 0)));
    if (y != null) halves.add(new HalfspaceRegion(new Vector(0, y, 0), new Vector(0, dir, 0)));
    if (z != null) halves.add(new HalfspaceRegion(new Vector(0, 0, z), new Vector(0, 0, dir)));

    switch (halves.size()) {
      case 0:
        throw new InvalidXMLException("Expected at least one of x, y, or z attributes", el);
      case 1:
        return halves.get(0);
      default:
        return new Intersect((Region[]) halves.toArray());
    }
  }

  @MethodParser("below")
  public RegionDefinition parseBelow(Element el) throws InvalidXMLException {
    return parseHalves(el, -1);
  }

  @MethodParser("above")
  public RegionDefinition parseAbove(Element el) throws InvalidXMLException {
    return parseHalves(el, 1);
  }

  @MethodParser("point")
  public PointRegion parsePoint(Element el) throws InvalidXMLException {
    return new PointRegion(XMLUtils.parseVector(new Node(el)));
  }

  @MethodParser("cuboid")
  public CuboidRegion parseCuboid(Element el) throws InvalidXMLException {
    Vector min = XMLUtils.parseVector(el.getAttribute("min"));
    Vector max = XMLUtils.parseVector(el.getAttribute("max"));
    Vector size = XMLUtils.parseVector(el.getAttribute("size"));

    if (min != null && max != null && size == null) {
      return new CuboidRegion(min, max);
    } else if (min != null && max == null && size != null) {
      return new CuboidRegion(min, min.clone().add(size));
    } else if (min == null && max != null && size != null) {
      return new CuboidRegion(max.clone().subtract(size), max);
    } else {
      throw new InvalidXMLException(
          "cuboid must specify exactly two of 'min', 'max', and 'size' attributes", el);
    }
  }

  @MethodParser("cylinder")
  public CylindricalRegion parseCylinder(Element el) throws InvalidXMLException {
    Vector base = XMLUtils.parseVector(el.getAttribute("base"));
    if (base == null) {
      throw new InvalidXMLException("Cylindrical region must specify valid base vector.", el);
    }
    try {
      double radius = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "radius"), Double.class, true);
      double height = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "height"), Double.class, true);
      return new CylindricalRegion(base, radius, height);
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Cylindrical region must specify valid radius and height.", el);
    }
  }

  @MethodParser("rectangle")
  public RectangleRegion parsePlanar(Element el) throws InvalidXMLException {
    Vector min = XMLUtils.parse2DVector(Node.fromRequiredAttr(el, "min"));
    Vector max = XMLUtils.parse2DVector(Node.fromRequiredAttr(el, "max"));
    return new RectangleRegion(min.getX(), min.getZ(), max.getX(), max.getZ());
  }

  @MethodParser("block")
  public BlockRegion parseBlock(Element el) throws InvalidXMLException {
    // TODO: remove "location" backwards compatibility with next major map proto bump
    Vector loc = XMLUtils.parseVector(Node.fromAttr(el, "location"));
    if (loc == null) {
      loc = XMLUtils.parseVector(new Node(el));
      if (loc == null) {
        throw new InvalidXMLException("Block region must have valid location vector.", el);
      }
    }
    return new BlockRegion(loc);
  }

  @MethodParser("union")
  public Region parseUnion(Element el) throws InvalidXMLException {
    return new Union(this.parseSubRegionsArray(el));
  }

  @MethodParser("intersect")
  public Region parseIntersect(Element el) throws InvalidXMLException {
    Region[] regions = this.parseSubRegionsArray(el);
    switch (regions.length) {
      case 0:
        throw new InvalidXMLException("Intersect must have at least one region.", el);
      case 1:
        return regions[0];
      default:
        return new Intersect(regions);
    }
  }

  @MethodParser("complement")
  public Complement parseComplement(Element el) throws InvalidXMLException {
    List<Region> regions = this.parseSubRegions(el);
    if (regions.size() < 2) {
      throw new InvalidXMLException("Complement requires at least 2 regions.", el);
    }
    return new Complement(
        regions.get(0), regions.subList(1, regions.size()).toArray(new Region[0]));
  }

  @MethodParser("negative")
  public NegativeRegion parseNegative(Element el) throws InvalidXMLException {
    return new NegativeRegion(this.parseChildren(el));
  }

  @MethodParser("circle")
  public CircleRegion parseCircle(Element el) throws InvalidXMLException {
    Vector center = XMLUtils.parse2DVector(Node.fromRequiredAttr(el, "center"));
    double radius = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "radius"), Double.class, true);
    return new CircleRegion(center.getX(), center.getZ(), radius);
  }

  @MethodParser("sphere")
  public SphereRegion parseSphere(Element el) throws InvalidXMLException {
    Vector origin = XMLUtils.parseVector(el.getAttribute("origin"));
    if (origin == null) {
      throw new InvalidXMLException("Sphere must specify a valid origin vector", el);
    }
    try {
      double radius = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "radius"), Double.class, true);
      return new SphereRegion(origin, radius);
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Sphere must specify a valid radius", el);
    }
  }

  @MethodParser("translate")
  public TranslatedRegion parseTranslate(Element el) throws InvalidXMLException {
    Attribute offsetAttribute = el.getAttribute("offset");
    if (offsetAttribute == null) {
      throw new InvalidXMLException("Translate region must have an offset", el);
    }
    Vector offset = XMLUtils.parseVector(offsetAttribute);
    return new TranslatedRegion(this.parseChildren(el), offset);
  }

  @MethodParser("mirror")
  public MirroredRegion parseMirror(Element el) throws InvalidXMLException {
    Vector normal = XMLUtils.parseVector(XMLUtils.getRequiredAttribute(el, "normal"));
    if (normal.lengthSquared() == 0) {
      throw new InvalidXMLException("normal must have a non-zero length", el);
    }

    Vector origin = XMLUtils.parseVector(el.getAttribute("origin"), new Vector());

    return new MirroredRegion(this.parseChildren(el), origin, normal);
  }

  @MethodParser("everywhere")
  public EverywhereRegion parseEverywhere(Element el) throws InvalidXMLException {
    return EverywhereRegion.INSTANCE;
  }

  @MethodParser("nowhere")
  public EmptyRegion parseNowhere(Element el) throws InvalidXMLException {
    return EmptyRegion.INSTANCE;
  }

  @MethodParser("empty")
  public EmptyRegion parseEmpty(Element el) throws InvalidXMLException {
    return EmptyRegion.INSTANCE;
  }
}
