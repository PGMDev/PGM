package tc.oc.pgm.map;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.xml.InvalidXMLException;

/**
 * Creates a {@link MapModule} from an XML document, or returns null to indicate that the module is
 * not needed.
 */
public interface MapModuleFactory<T extends MapModule> {
  @Nullable
  T parse(MapModuleContext context, Logger logger, Document doc) throws InvalidXMLException;
}
