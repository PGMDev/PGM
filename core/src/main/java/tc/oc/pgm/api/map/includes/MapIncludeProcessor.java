package tc.oc.pgm.api.map.includes;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.xml.InvalidXMLException;

/** A processor to determine which {@link MapInclude}s should be included when loading a map * */
public interface MapIncludeProcessor {

  /**
   * Process the given {@link Document} and return a collection of {@link MapInclude}s.
   *
   * @param element An include element within a map document
   * @return element collection of map includes, collection will be empty if none are found.
   * @throws InvalidXMLException If the given document is not found or able to be parsed.
   */
  MapInclude getMapInclude(Element element) throws InvalidXMLException;

  /**
   * Get a {@link MapInclude} by its id
   *
   * @param includeId ID of the map include
   * @return A {@link MapInclude}
   */
  @Nullable
  MapInclude getMapIncludeById(String includeId);

  /**
   * Get a {@link MapInclude} that is global and applies to all maps
   *
   * @return A global {@link MapInclude} if any is present
   */
  @Nullable
  MapInclude getGlobalInclude();

  /** Reload the processor to fetch new map includes. */
  void loadNewIncludes();
}
