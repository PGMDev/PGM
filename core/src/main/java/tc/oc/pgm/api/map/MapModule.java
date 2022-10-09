package tc.oc.pgm.api.map;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.Module;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.util.xml.InvalidXMLException;

/**
 * A configuration provider that creates {@link MatchModule}s.
 *
 * <p>No references to {@link org.bukkit.entity.Player} or {@link org.bukkit.World} objects should
 * be held in a {@link MapModule}. That is the responsibility of a {@link MatchModule}.
 */
public interface MapModule<T extends MatchModule> extends Module, MatchModuleFactory<T> {

  /** @see MatchModuleFactory#createMatchModule(Match) */
  @Nullable
  T createMatchModule(Match match) throws ModuleLoadException;

  /**
   * Get a collection of {@link MapTag}s to describe this module.
   *
   * @return A collection of {@link MapTag}s.
   */
  default Collection<MapTag> getTags() {
    return Collections.emptyList();
  }

  /**
   * Callback when all {@link MapModule}s were successfully loaded.
   *
   * @param factory A map factory, can be used to get other {@link MapModule}s.
   * @param logger A logger, also viewable from in-game.
   * @param doc A document.
   * @throws InvalidXMLException If a post-parse error occurs.
   */
  default void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {}
}
