package tc.oc.pgm.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.SAXHandler;

public class MapFilePreprocessor {

  private static final ThreadLocal<SAXBuilder> DOCUMENT_FACTORY =
      ThreadLocal.withInitial(
          () -> {
            final SAXBuilder builder = new SAXBuilder();
            builder.setSAXHandlerFactory(SAXHandler.FACTORY);
            return builder;
          });

  private final MapIncludeProcessor includes;
  private final MapSource source;

  public static Document getDocument(MapSource source, MapIncludeProcessor includes)
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    return new MapFilePreprocessor(source, includes).getDocument();
  }

  private MapFilePreprocessor(MapSource source, MapIncludeProcessor includes) {
    this.source = source;
    this.includes = includes;
  }

  public Document getDocument()
      throws MapMissingException, IOException, JDOMException, InvalidXMLException {
    Document document;
    try (final InputStream stream = source.getDocument()) {
      document = DOCUMENT_FACTORY.get().build(stream);
      document.setBaseURI(source.getId());
    }

    // TODO: preprocess document, apply includes and conditionals for the current variant
    // Inspiration on how to better and recursively apply that:
    // https://github.com/OvercastNetwork/ProjectAres/blob/master/PGM/src/main/java/tc/oc/pgm/map/MapFilePreprocessor.java#L171

    // Check for any included map sources, appending them to the document if present
    Collection<MapInclude> mapIncludes = includes.getMapIncludes(document);
    for (MapInclude include : mapIncludes) {
      document.getRootElement().addContent(0, include.getContent());
    }
    source.setIncludes(mapIncludes);

    return document;
  }
}
