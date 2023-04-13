package tc.oc.pgm.util.compose;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

/** Parses a {@link Composition<T>} */
public abstract class CompositionParser<T> {

  protected final MapFactory factory;

  public CompositionParser(MapFactory factory) {
    this.factory = factory;
  }

  public abstract T parseUnit(Element element) throws InvalidXMLException;

  public Composition<T> parseElement(Element element) throws InvalidXMLException {
    return parseElementList(element.getChildren());
  }

  public Composition<T> parseElementList(List<Element> elements) throws InvalidXMLException {
    switch (elements.size()) {
      case 0:
        return new None<>();
      case 1:
        return parseAtom(elements.get(0));
      default:
        {
          List<Composition<T>> compositions = new ArrayList<>(elements.size());
          for (Element element : elements) {
            compositions.add(this.parseAtom(element));
          }
          return new All<>(compositions);
        }
    }
  }

  public Composition<T> parseAtom(Element element) throws InvalidXMLException {
    switch (element.getName()) {
      case "none":
        return new None<>();

      case "maybe":
        return new Maybe<>(
            this.factory.getFilters().parseFilterProperty(element, "filter"),
            parseElementList(element.getChildren()));
      case "all":
        return parseElementList(element.getChildren());

      case "any":
        return new Any<>(
            XMLUtils.parseBoundedNumericRange(
                element.getAttribute("count"), Integer.class, Range.singleton(1)),
            XMLUtils.parseBoolean(element.getAttribute("unique"), true),
            this.parseOptions(element.getChildren()));
      default:
        return new Unit<>(this.parseUnit(element));
    }
  }

  private List<Any.Option<T>> parseOptions(List<Element> elements) throws InvalidXMLException {
    List<Any.Option<T>> options = new ArrayList<>(elements.size());
    for (Element element : elements) {
      options.add(this.parseOption(element));
    }

    return options;
  }

  Any.Option<T> parseOption(Element element) throws InvalidXMLException {
    if ("option".equals(element.getName())) {
      return new Any.Option<>(
          XMLUtils.parseNumber(element.getAttribute("weight"), Double.class, 1D),
          this.factory.getFilters().parseFilterProperty(element, "filter", StaticFilter.ALLOW),
          parseElementList(element.getChildren()));
    } else {
      return new Any.Option<>(1, StaticFilter.ALLOW, parseAtom(element));
    }
  }
}
