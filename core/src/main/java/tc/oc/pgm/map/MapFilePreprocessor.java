package tc.oc.pgm.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.SAXHandler;

public class MapFilePreprocessor {

  private static final ThreadLocal<SAXBuilder> DOCUMENT_FACTORY =
      ThreadLocal.withInitial(
          () -> {
            final SAXBuilder builder = new SAXBuilder();
            builder.setSAXHandlerFactory(SAXHandler.FACTORY);
            return builder;
          });

  private final MapIncludeProcessor includeProcessor;
  private final MapSource source;
  private final String variant;
  private final List<MapInclude> includes;

  public static Document getDocument(MapSource source, MapIncludeProcessor includes)
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    return new MapFilePreprocessor(source, includes).getDocument();
  }

  private MapFilePreprocessor(MapSource source, MapIncludeProcessor includeProcessor) {
    this.source = source;
    this.includeProcessor = includeProcessor;
    this.variant = source.getVariant() == null ? "default" : source.getVariant();
    this.includes = new ArrayList<>();
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

    processChildren(document.getRootElement());

    source.setIncludes(includes);
    return document;
  }

  private void processChildren(Element parent) throws InvalidXMLException {
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
        processChildren(child);
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
        Arrays.asList(Node.fromRequiredAttr(el, "variant").getValue().split("[\\\\s,]"))
            .contains(variant);

    return contains == shouldContain ? el.cloneContent() : Collections.emptyList();
  }
}
