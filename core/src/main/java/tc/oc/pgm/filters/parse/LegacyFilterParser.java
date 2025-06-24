package tc.oc.pgm.filters.parse;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.matcher.block.MaterialFilter;
import tc.oc.pgm.filters.matcher.party.TeamFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
import tc.oc.pgm.filters.operator.FilterNode;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

/** For proto < 1.4 */
public class LegacyFilterParser extends FilterParser {

  protected final FilterContext filterContext = new FilterContext();

  public LegacyFilterParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public FilterContext getUsedContext() {
    return this.filterContext;
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
  public Filter parseReference(Node node, String id) throws InvalidXMLException {
    Filter filter = this.filterContext.get(id);
    if (filter == null) {
      throw new InvalidXMLException("No filter named '" + id + "'", node);
    }
    return filter;
  }

  @Override
  public void validate(Filter filter, FeatureValidation<FilterDefinition> validation, Node node)
      throws InvalidXMLException {
    validation.validate((FilterDefinition) filter, node);
  }

  @MethodParser("filter")
  public Filter parseFilter(Element el) throws InvalidXMLException {
    if (isReference(el)) {
      return parseReference(Node.fromAttr(el, "name"));
    } else {
      return this.parseAll(el);
    }
  }

  // Legacy has separate context, team wouldn't be found in the filter context.
  @MethodParser("team")
  public TeamFilter parseTeam(Element el) throws InvalidXMLException {
    return new TeamFilter(Teams.getTeamRef(new Node(el), this.factory));
  }

  // Legacy not allows for multiple children and is an implicit and
  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(AnyFilter.of(parseChildren(el)));
  }

  // Removed in proto 1.4 to avoid conflict with <block> region
  @MethodParser("block")
  public Filter parseBlock(Element el) throws InvalidXMLException {
    MaterialMatcher pattern = MaterialMatcher.parse(el);
    if (!pattern.getMaterials().iterator().next().isBlock()) {
      throw new InvalidXMLException("Material is not a block", el);
    }
    return new MaterialFilter(pattern);
  }
}
