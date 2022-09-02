package tc.oc.pgm.api.map.includes;

import java.util.Collection;
import org.jdom2.Document;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.util.xml.InvalidXMLException;

/** A processor to determine which {@link MapInclude}s should be included when loading a map * */
public interface MapIncludeProcessor {

  /**
   * Process the given {@link Document} and return a collection of {@link MapInclude}s.
   *
   * @param document A map document
   * @return A collection of map includes, collection will be empty if none are found.
   * @throws InvalidXMLException If the given document is not found or able to be parsed.
   */
  Collection<MapInclude> getMapIncludes(Document document) throws InvalidXMLException;

  /**
   * Get a {@link MapInclude} by its id
   *
   * @param includeId ID of the map include
   * @return A {@link MapInclude}
   */
  MapInclude getMapIncludeById(String includeId);

  /**
   * Reload the processor to fetch new map includes or reload existing ones.
   *
   * @param config A configuration file.
   */
  void reload(Config config);
}
