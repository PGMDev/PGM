package tc.oc.pgm.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.SAXHandler;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapFilePreprocessor {

  private static final ThreadLocal<SAXBuilder> DOCUMENT_FACTORY =
      ThreadLocal.withInitial(
          () -> {
            final SAXBuilder builder = new SAXBuilder();
            builder.setSAXHandlerFactory(SAXHandler.FACTORY);
            return builder;
          });

  private static final Pattern CONSTANT_PATTERN = Pattern.compile("\\$\\{(.+)}");

  private final MapIncludeProcessor includeProcessor;
  private final MapSource source;
  private final String variant;
  private final List<MapInclude> includes;

  private final Map<String, String> constants;

  public static Document getDocument(MapSource source, MapIncludeProcessor includes)
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    return new MapFilePreprocessor(source, includes).getDocument();
  }

  private MapFilePreprocessor(MapSource source, MapIncludeProcessor includeProcessor) {
    this.source = source;
    this.includeProcessor = includeProcessor;
    this.variant = source.getVariant() == null ? "default" : source.getVariant();
    this.includes = new ArrayList<>();
    this.constants = new HashMap<>();
  }

  public Document getDocument()
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    Document document;
    try (final InputStream stream = source.getDocument()) {
      document = DOCUMENT_FACTORY.get().build(stream);
      document.setBaseURI(source.getId());
    }

    MapInclude global = includeProcessor.getGlobalInclude();
    if (global != null) {
      document.getRootElement().addContent(0, global.getContent());
      includes.add(global);
    }

    preprocessChildren(document.getRootElement());
    source.setIncludes(includes);

    for (Element constant :
        XMLUtils.flattenElements(document.getRootElement(), "constants", "constant", 0)) {
      constants.put(XMLUtils.parseRequiredId(constant), constant.getText());
    }

    // If no constants are set, assume we can skip the step
    if (constants.size() > 0) {
      postprocessChildren(document.getRootElement());
    }

    return document;
  }

  private void preprocessChildren(Element parent) throws InvalidXMLException {
    for (int i = 0; i < parent.getContentSize(); i++) {
      Content content = parent.getContent(i);
      if (!(content instanceof Element)) continue;

      Element child = (Element) content;
      List<Content> replacement = null;

      switch (child.getName()) {
        case "include":
          replacement = processIncludeElement(child);
          break;
        case "if":
          replacement = processConditional(child, true);
          break;
        case "unless":
          replacement = processConditional(child, false);
          break;
      }

      if (replacement != null) {
        parent.removeContent(i);
        parent.addContent(i, replacement);
        i--; // Process replacement content
      } else {
        preprocessChildren(child);
      }
    }
  }

  private List<Content> processIncludeElement(Element element) throws InvalidXMLException {
    MapInclude include = includeProcessor.getMapInclude(element);
    if (include != null) {
      includes.add(include);
      return include.getContent();
    }
    return Collections.emptyList();
  }

  private List<Content> processConditional(Element el, boolean shouldContain)
      throws InvalidXMLException {
    boolean contains =
        Arrays.asList(Node.fromRequiredAttr(el, "variant").getValue().split("[\\s,]+"))
            .contains(variant);

    return contains == shouldContain ? el.cloneContent() : Collections.emptyList();
  }

  private void postprocessChildren(Element parent) throws InvalidXMLException {
    for (Attribute attribute : parent.getAttributes()) {
      attribute.setValue(postprocessString(parent, attribute.getValue()));
    }

    for (int i = 0; i < parent.getContentSize(); i++) {
      Content content = parent.getContent(i);
      if (content instanceof Element) {
        postprocessChildren((Element) content);
      } else if (content instanceof Text) {
        Text text = (Text) content;
        text.setText(postprocessString(parent, text.getText()));
      }
    }
  }

  private String postprocessString(Element el, String text) throws InvalidXMLException {
    Matcher matcher = CONSTANT_PATTERN.matcher(text);

    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String constant = matcher.group(1);
      String replacement = constants.get(matcher.group(1));
      if (replacement == null)
        throw new InvalidXMLException(
            "No constant '" + constant + "' is used but has not been defined", el);

      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
