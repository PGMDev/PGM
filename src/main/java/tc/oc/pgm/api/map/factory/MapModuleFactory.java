package tc.oc.pgm.api.map.factory;

import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.module.ModuleFactory;
import tc.oc.xml.InvalidXMLException;

import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * A factory for creating {@link MapModule}s and its dependency graph.
 *
 * @param <T> Specific type of {@link MapModule}.
 */
public interface MapModuleFactory<T extends MapModule> extends ModuleFactory<MapModule> {

  /**
   * Parses a {@link Document} to create a {@link MapModule}.
   *
   * @param factory A map context, can be used to get other {@link MapModule}s.
   * @param logger A logger, also viewable from in-game.
   * @param doc A document.
   * @return A {@link MapModule} or {@code null} to silently fail.
   * @throws InvalidXMLException If the {@link Document} could not be parsed.
   */
  @Nullable
  T parse(MapFactory factory, Logger logger, Document doc) throws InvalidXMLException;
}
