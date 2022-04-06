package tc.oc.pgm.api.filter;

import java.util.List;
import javax.annotation.Nullable;
import org.jdom2.Element;
import tc.oc.pgm.api.MethodParser;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

public interface FilterParser {
  /**
   * The top-level method for parsing an individual filter element. This method should call {@link
   * #parseDynamic} at some point, and should also take care of adding the filter to whatever type
   * of context is in use.
   */
  Filter parse(Element el) throws InvalidXMLException;

  /**
   * Return the filter referenced by the given name/id, and assume it appears in the given {@link
   * Node} for error reporting purposes.
   */
  Filter parseReference(Node node, String value) throws InvalidXMLException;

  Filter parseReference(Node node) throws InvalidXMLException;

  boolean isFilter(Element el);

  List<Element> getFilterChildren(Element parent);

  List<Filter> parseFilterChildren(Element parent) throws InvalidXMLException;

  Filter parseChild(Element parent) throws InvalidXMLException;

  List<Filter> parseChildren(Element parent) throws InvalidXMLException;

  @Nullable
  Filter parseFilterProperty(Element el, String name) throws InvalidXMLException;

  Filter parseRequiredFilterProperty(Element el, String name) throws InvalidXMLException;

  Filter parseFilterProperty(Element el, String name, @Nullable Filter def)
      throws InvalidXMLException;

  List<Filter> parseFiltersProperty(Element el, String name) throws InvalidXMLException;

  @MethodParser("always")
  Filter parseAlways(Element el);

  @MethodParser("never")
  Filter parseNever(Element el);

  @MethodParser("any")
  Filter parseAny(Element el) throws InvalidXMLException;

  @MethodParser("all")
  Filter parseAll(Element el) throws InvalidXMLException;

  @MethodParser("one")
  Filter parseOne(Element el) throws InvalidXMLException;

  @MethodParser("not")
  Filter parseNot(Element el) throws InvalidXMLException;

  @MethodParser("material")
  Filter parseMaterial(Element el) throws InvalidXMLException;
}
