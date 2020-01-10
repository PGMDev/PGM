package tc.oc.pgm.api.map.factory;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.module.ModuleFactory;
import tc.oc.xml.InvalidXMLException;

/**
 * A factory for creating {@link MapModule}s and its dependency graph.
 *
 * @param <T> Specific type of {@link MapModule}.
 */
public interface MapModuleFactory<T extends MapModule> extends ModuleFactory<MapModule> {

  /**
   * Parses a {@link Document} to create a {@link MapModule}.
   *
   * @param context A map context, can be used to get other {@link MapModule}s.
   * @param logger A logger, also viewable from in-game.
   * @param doc A document.
   * @return A {@link MapModule} or {@code null} to silently fail.
   * @throws InvalidXMLException If the {@link Document} could not be parsed.
   */
  @Nullable
  T parse(MapContext context, Logger logger, Document doc) throws InvalidXMLException;
}
