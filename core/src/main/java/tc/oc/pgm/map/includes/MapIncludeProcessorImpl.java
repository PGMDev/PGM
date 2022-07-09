package tc.oc.pgm.map.includes;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.SAXHandler;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapIncludeProcessorImpl implements MapIncludeProcessor {

  private final Logger logger;
  private final Set<MapInclude> includes;

  protected static final ThreadLocal<SAXBuilder> DOCUMENT_FACTORY =
      ThreadLocal.withInitial(
          () -> {
            final SAXBuilder builder = new SAXBuilder();
            builder.setSAXHandlerFactory(SAXHandler.FACTORY);
            return builder;
          });

  public MapIncludeProcessorImpl(Logger logger) {
    this.logger = logger;
    this.includes = Sets.newHashSet();
  }

  @Nullable
  private MapInclude getIncludeById(String id) {
    return includes.stream()
        .filter(include -> include.getId().equalsIgnoreCase(id))
        .findAny()
        .orElse(null);
  }

  @Override
  public MapInclude getGlobalInclude() {
    return getIncludeById("global");
  }

  @Override
  public Collection<MapInclude> getMapIncludes(Document document) throws InvalidXMLException {
    Set<MapInclude> mapIncludes = Sets.newHashSet();
    List<Element> elements = document.getRootElement().getChildren("include");
    for (Element element : elements) {

      String legacy = XMLUtils.getNullableAttribute(element, "src");
      if (legacy != null) {
        // Send a warning to legacy include statements without preventing them from loading
        logger.warning(
            "["
                + document.getBaseURI()
                + "] "
                + "Legacy include statements are no longer supported, please upgrade to the <include id='name'/> format.");
        return Sets.newHashSet();
      }

      String id = XMLUtils.getRequiredAttribute(element, "id").getValue();
      MapInclude include = getIncludeById(id);
      if (include == null)
        throw new InvalidXMLException(
            "The provided include id '" + id + "' could not be found!", element);

      mapIncludes.add(include);
    }
    return mapIncludes;
  }

  @Override
  public void reload(Config config) {
    this.includes.clear();

    if (config.getIncludesDirectory() == null) return;

    File includeFiles = new File(config.getIncludesDirectory());
    if (!includeFiles.isDirectory()) {
      logger.warning(config.getIncludesDirectory() + " is not a directory!");
      return;
    }
    File[] files = includeFiles.listFiles();
    for (File file : files) {
      try {
        this.includes.add(new MapIncludeImpl(file));
      } catch (MapMissingException | JDOMException | IOException error) {
        logger.info("Unable to load " + file.getName() + " include document");
        error.printStackTrace();
      }
    }
  }
}
