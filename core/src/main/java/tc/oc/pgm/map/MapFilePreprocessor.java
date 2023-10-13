package tc.oc.pgm.map;

import static tc.oc.pgm.api.map.MapSource.DEFAULT_VARIANT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.DocumentWrapper;
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

  private static final Pattern CONSTANT_PATTERN = Pattern.compile("\\$\\{(.+?)}");

  private final MapIncludeProcessor includeProcessor;
  private final MapSource source;
  private final String variant;
  private final List<MapInclude> includes;

  private final Map<String, String> constants;
  private final Set<String> variantIds;

  public static Document getDocument(MapSource source, MapIncludeProcessor includes)
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    return new MapFilePreprocessor(source, includes).getDocument();
  }

  private MapFilePreprocessor(MapSource source, MapIncludeProcessor includeProcessor) {
    this.source = source;
    this.includeProcessor = includeProcessor;
    this.variant = source.getVariantId();
    this.includes = new ArrayList<>();
    this.constants = new HashMap<>();
    this.variantIds = new HashSet<>();
  }

  public Document getDocument()
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    DocumentWrapper document;
    try (final InputStream stream = source.getDocument()) {
      document = (DocumentWrapper) DOCUMENT_FACTORY.get().build(stream);
      document.setBaseURI(source.getId());
    }

    variantIds.add(DEFAULT_VARIANT);
    for (Element variant : document.getRootElement().getChildren("variant")) {
      variantIds.add(XMLUtils.parseRequiredId(variant));
    }

    document.runWithoutVisitation(
        () -> {
          MapInclude global = includeProcessor.getGlobalInclude();
          if (global != null) {
            document.getRootElement().addContent(0, global.getContent());
            includes.add(global);
          }

          preprocessChildren(document.getRootElement());
          source.setIncludes(includes);
        });

    for (Element constant :
        XMLUtils.flattenElements(document.getRootElement(), "constants", "constant", 0)) {
      boolean isDelete = XMLUtils.parseBoolean(constant.getAttribute("delete"), false);
      String text = constant.getText();
      if ((text == null || text.isEmpty()) != isDelete)
        throw new InvalidXMLException(
            "Delete attribute cannot be combined with having an inner text", constant);
      constants.put(XMLUtils.parseRequiredId(constant), isDelete ? null : constant.getText());
    }

    // If no constants are set, assume we can skip the step
    if (!constants.isEmpty()) {
      document.runWithoutVisitation(() -> postprocessChildren(document.getRootElement()));
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

    Node node = Node.fromRequiredAttr(el, "variant", "has-variant");
    List<String> filter = Arrays.asList(node.getValue().split("[\\s,]+"));

    boolean contains =
        "variant".equals(node.getName())
            ? filter.contains(this.variant)
            : filter.stream().anyMatch(variantIds::contains);

    return contains == shouldContain ? el.cloneContent() : Collections.emptyList();
  }

  private void postprocessChildren(Element parent) throws InvalidXMLException {
    for (Attribute attribute : parent.getAttributes()) {
      String result = postprocessString(parent, attribute.getValue());
      if (result == null) parent.removeAttribute(attribute);
      else attribute.setValue(result);
    }

    for (int i = 0; i < parent.getContentSize(); i++) {
      Content content = parent.getContent(i);
      if (content instanceof Element) {
        postprocessChildren((Element) content);
      } else if (content instanceof Text) {
        Text text = (Text) content;

        String result = postprocessString(parent, text.getText());
        if (result == null) parent.removeContent(text);
        else text.setText(result);
      }
    }
  }

  private @Nullable String postprocessString(Element el, String text) throws InvalidXMLException {
    Matcher matcher = CONSTANT_PATTERN.matcher(text);

    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String constant = matcher.group(1);
      String replacement = constants.get(constant);
      if (replacement == null) {
        if (!constants.containsKey(constant)) {
          throw new InvalidXMLException(
              "Constant '" + constant + "' is used but has not been defined", el);
        } else {
          return null;
        }
      }
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
