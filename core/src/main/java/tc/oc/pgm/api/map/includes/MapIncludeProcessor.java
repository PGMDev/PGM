package tc.oc.pgm.api.map.includes;

import java.util.Collection;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.util.xml.InvalidXMLException;

/** A processor to determine which {@link MapInclude}s should be included when loading a map * */
public interface MapIncludeProcessor {

  /**
   * Get a {@link MapInclude} which should be included on all maps or null if none.
   *
   * @return a {@link MapInclude}
   */
  @Nullable
  MapInclude getGlobalInclude();

  /**
   * Process the given {@link Document} and return a collection of {@link MapInclude}s.
   *
   * @param document A map document
   * @return A collection of map includes, collection will be empty if none are found.
   * @throws InvalidXMLException If the given document is not found or able to be parsed.
   */
  Collection<MapInclude> getMapIncludes(Document document) throws InvalidXMLException;

  /**
   * Reload the processor to fetch new map includes or reload existing ones.
   *
   * @param config A configuration file.
   */
  void reload(Config config);
}
