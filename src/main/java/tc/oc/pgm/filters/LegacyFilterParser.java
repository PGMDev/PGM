package tc.oc.pgm.filters;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

/** For proto < 1.4 */
public class LegacyFilterParser extends FilterParser {

  protected final FilterContext filterContext = new FilterContext();

  public LegacyFilterParser(MapContext context) {
    super(context);
  }

  @Override
  public Filter parse(Element el) throws InvalidXMLException {
    Filter filter;
    // FIXME: compat until map proto 2.0.0
    if (el.getAttribute("parents") != null
        || el.getChild("allow") != null
        || el.getChild("deny") != null) {
      // compatibility mode
      List<Filter> parents = this.parseParents(el);
      List<Filter> allowedMatchers = this.parseGrandchildren(el, "allow");
      List<Filter> deniedMatchers = this.parseGrandchildren(el, "deny");
      filter = new FilterNode(parents, allowedMatchers, deniedMatchers);
    } else {
      filter = this.parseDynamic(el);
    }

    if (el.getAttribute("name") != null && !isReference(el) && !(filter instanceof Region)) {
      this.filterContext.add(el.getAttributeValue("name"), filter);
    } else {
      this.filterContext.add(filter);
    }

    return filter;
  }

  protected List<Filter> parseGrandchildren(Element parent, String childName)
      throws InvalidXMLException {
    List<Filter> filters = Lists.newArrayList();
    for (Element child : parent.getChildren(childName)) {
      filters.addAll(this.parseChildren(child));
    }
    return filters;
  }

  protected List<Filter> parseParents(Element el) throws InvalidXMLException {
    List<Filter> parents = new ArrayList<Filter>();
    if (el.getAttribute("parents") == null) {
      return parents;
    }
    String[] parentNames = el.getAttributeValue("parents").split(" ");
    for (String name : parentNames) {
      Filter filter = this.filterContext.get(name);
      if (filter == null) {
        throw new InvalidXMLException("Parent '" + name + "' can not be found", el);
      } else {
        parents.add(filter);
      }
    }
    return parents;
  }

  protected boolean isReference(Element el) {
    return el.getName().equalsIgnoreCase("filter")
        && el.getChildren().isEmpty()
        && el.getAttribute("parents") == null
        && el.getAttribute("name") != null;
  }

  @Override
  public Filter parseReference(Node node, String value) throws InvalidXMLException {
    Filter filter = this.filterContext.get(value);
    if (filter == null) {
      throw new InvalidXMLException("No filter named '" + value + "'", node);
    }
    return filter;
  }

  @MethodParser("filter")
  public Filter parseFilter(Element el) throws InvalidXMLException {
    if (isReference(el)) {
      return parseReference(Node.fromAttr(el, "name"));
    } else {
      return this.parseAll(el);
    }
  }

  // Removed in proto 1.4 to avoid conflict with <block> region
  @MethodParser("block")
  public BlockFilter parseBlock(Element el) throws InvalidXMLException {
    SingleMaterialMatcher pattern = XMLUtils.parseMaterialPattern(el);
    if (!pattern.getMaterial().isBlock()) {
      throw new InvalidXMLException("Material is not a block", el);
    }
    return new BlockFilter(pattern);
  }
}
