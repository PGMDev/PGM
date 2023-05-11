package tc.oc.pgm.map.includes;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.SAXHandler;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapIncludeProcessorImpl implements MapIncludeProcessor {

  private final Logger logger;
  private final Map<String, MapInclude> includes;

  protected static final ThreadLocal<SAXBuilder> DOCUMENT_FACTORY =
      ThreadLocal.withInitial(
          () -> {
            final SAXBuilder builder = new SAXBuilder();
            builder.setSAXHandlerFactory(SAXHandler.FACTORY);
            return builder;
          });

  public MapIncludeProcessorImpl(Logger logger) {
    this.logger = logger;
    this.includes = Maps.newHashMap();
  }

  public MapInclude getGlobalInclude() {
    return getMapIncludeById("global");
  }

  @Override
  public @Nullable MapInclude getMapIncludeById(String includeId) {
    return includes.get(includeId);
  }

  @Override
  public MapInclude getMapInclude(Element element) throws InvalidXMLException {
    if (Node.fromAttr(element, "src") != null) {
      // Send a warning to legacy include statements without preventing them from loading
      logger.warning(
          "["
              + element.getDocument().getBaseURI()
              + "] "
              + "Legacy include statements are no longer supported, please upgrade to the <include id='name'/> format.");
      return null;
    }

    String id = XMLUtils.getRequiredAttribute(element, "id").getValue();
    MapInclude include = getMapIncludeById(id);
    if (include == null)
      throw new InvalidXMLException(
          "The provided include id '" + id + "' could not be found!", element);
    return include;
  }

  @Override
  public void loadNewIncludes() {
    Config config = PGM.get().getConfiguration();
    if (config.getIncludesDirectory() == null) return;

    File includeFiles = config.getIncludesDirectory().toFile();
    if (!includeFiles.isDirectory()) {
      logger.warning(config.getIncludesDirectory() + " is not a directory!");
      return;
    }

    Set<String> deletedIncludes = new HashSet<>(includes.keySet());

    File[] files = includeFiles.listFiles();
    for (File file : files) {
      String filename = file.getName();
      if (!filename.endsWith(".xml")) continue;

      String id = filename.substring(0, filename.length() - ".xml".length());
      // Already loaded, can ignore and continue
      if (deletedIncludes.remove(id)) continue;

      try {
        this.includes.put(id, new MapIncludeImpl(file));
      } catch (MapMissingException | JDOMException | IOException error) {
        logger.log(Level.WARNING, "Failed to load " + filename + " include document", error);
        error.printStackTrace();
      }
    }

    for (String id : deletedIncludes) {
      this.includes.remove(id);
      logger.info("Removed deleted include file " + id);
    }
  }
}
