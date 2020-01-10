package tc.oc.pgm.points;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.regions.*;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.collection.ArrayUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

/**
 * PointProvider grammar is a bit strange due to backward compatibility. The root element is what
 * the caller passes to {@link #parse}, and it can have any name at all. This element can either be
 * a container of PointProvider sub-elements, or a {@link PointRegion}. Child elements are parsed as
 * {@link Region}s and wrapped in {@link RegionPointProvider}s, EXCEPT for <point>s, which are
 * treated the same as the root element.
 */
public class PointParser {
  final MapContext context;
  final RegionParser regionParser;

  public PointParser(MapContext context) {
    this.context = checkNotNull(context);
    this.regionParser = context.legacy().getRegions();
  }

  private Region validate(Region region, Node node) throws InvalidXMLException {
    regionParser.validate(region, RandomPointsValidation.INSTANCE, node);
    return region;
  }

  /** Parse any number of {@link PointProvider}s in attributes or children of the given names. */
  public List<PointProvider> parseMultiProperty(
      Element el, PointProviderAttributes attributes, String name, String... aliases)
      throws InvalidXMLException {
    aliases = ArrayUtils.append(aliases, name);
    attributes = parseAttributes(el, attributes);

    List<PointProvider> providers = new ArrayList<>();
    for (Attribute attr : XMLUtils.getAttributes(el, aliases)) {
      providers.add(
          new RegionPointProvider(
              validate(regionParser.parseReference(attr), new Node(attr)), attributes));
    }
    for (Element child : XMLUtils.getChildren(el, aliases)) {
      providers.add(
          new RegionPointProvider(
              validate(regionParser.parseChild(child), new Node(child)),
              parseAttributes(child, attributes)));
    }
    return providers;
  }

  /**
   * Parse the given element as a container for {@link PointProvider}s. The given element is not
   * itself parsed as a PointProvider, but its attributes are inherited by any contained
   * PointProviders.
   */
  public List<PointProvider> parseChildren(Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    return parseChildren(new ArrayList<PointProvider>(), el, attributes);
  }

  /** Parse the given element as a {@link PointProvider} or container of PointProviders. */
  public List<PointProvider> parse(Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    return parsePoint(new ArrayList<PointProvider>(), el, attributes);
  }

  private List<PointProvider> parsePoint(
      List<PointProvider> providers, Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    attributes = parseAttributes(el, attributes);
    if (el.getChildren().isEmpty()) {
      // If it has no children, parse it as a Point region (regardless of the element name)
      providers.add(new RegionPointProvider(regionParser.parsePoint(el), attributes));
    } else {
      // If it does have children, parse it as a container
      parseChildren(providers, el, attributes);
    }
    return providers;
  }

  private List<PointProvider> parseChildren(
      List<PointProvider> providers, Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    attributes = parseAttributes(el, attributes);
    for (Element elChild : el.getChildren()) {
      parseChild(providers, elChild, attributes);
    }
    return providers;
  }

  private List<PointProvider> parseChild(
      List<PointProvider> providers, Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    attributes = parseAttributes(el, attributes);
    if ("point".equals(el.getName())) {
      // For legacy compatibility, <point> is treated specially
      parsePoint(providers, el, attributes);
    } else {
      // Anything else is parsed as a region
      parseRegion(providers, el, attributes);
    }
    return providers;
  }

  private void parseRegion(
      List<PointProvider> providers, Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    Node node = new Node(el);
    for (Region region : expandRegion(new ArrayList<Region>(), regionParser.parse(el))) {
      providers.add(new RegionPointProvider(validate(region, node), attributes));
    }
  }

  private List<Region> expandRegion(List<Region> expanded, Region region)
      throws InvalidXMLException {
    if (region instanceof Union) {
      // Special case to allow Unions to be used as sets of regions,
      // which can avoid a lot of repetitive XML.
      for (Region child : ((Union) region).getRegions()) {
        expandRegion(expanded, child);
      }
    } else {
      expanded.add(region);
    }

    return expanded;
  }

  AngleProvider parseStaticAngleProvider(Attribute attr) throws InvalidXMLException {
    Float angle = XMLUtils.parseNumber(attr, Float.class, (Float) null);
    return angle == null ? null : new StaticAngleProvider(angle);
  }

  public PointProviderAttributes parseAttributes(Element el, PointProviderAttributes attributes)
      throws InvalidXMLException {
    boolean safe = XMLUtils.parseBoolean(el.getAttribute("safe"), attributes.isSafe());
    boolean outdoors = XMLUtils.parseBoolean(el.getAttribute("outdoors"), attributes.isOutdoors());

    Vector target = XMLUtils.parseVector(el.getAttribute("angle"), (Vector) null);
    if (target != null) {
      return new PointProviderAttributes(
          new DirectedYawProvider(target), new DirectedPitchProvider(target), safe, outdoors);
    }

    AngleProvider yawProvider = parseStaticAngleProvider(el.getAttribute("yaw"));
    AngleProvider pitchProvider = parseStaticAngleProvider(el.getAttribute("pitch"));
    if (yawProvider != null || pitchProvider != null || safe != attributes.isSafe()) {
      return new PointProviderAttributes(yawProvider, pitchProvider, safe, outdoors);
    }

    return attributes;
  }
}
