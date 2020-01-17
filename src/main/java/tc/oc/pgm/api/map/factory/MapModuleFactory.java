package tc.oc.pgm.api.map.factory;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.module.ModuleFactory;
import tc.oc.xml.InvalidXMLException;

/** A factory for creating {@link MapModule}s from a {@link Document}. */
public interface MapModuleFactory<T extends MapModule> extends ModuleFactory<MapModule> {

  /**
   * Parses a {@link Document} to create a {@link MapModule}.
   *
   * @param factory A map factory, can be used to get other {@link MapModule}s.
   * @param logger A logger, also viewable from in-game.
   * @param doc A document.
   * @return A {@link MapModule} or {@code null} to silently skip loading.
   * @throws InvalidXMLException If there was a parsing error.
   */
  @Nullable
  T parse(MapFactory factory, Logger logger, Document doc) throws InvalidXMLException;
}
