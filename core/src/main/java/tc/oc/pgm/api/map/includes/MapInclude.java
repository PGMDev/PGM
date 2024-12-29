package tc.oc.pgm.api.map.includes;

import java.util.List;
import org.jdom2.Content;

/** Represents a snippet of XML that can be referenced for reuse * */
public interface MapInclude {

  /**
   * Get the system file time from when this MapInclude file was last modified.
   *
   * @return Time of last file modification
   */
  long getLastModified();

  /**
   * Get a collection of {@link Content} which can be merged into an existing
   * {@link org.jdom2.Document}. If the underlying file has changed, it will re-load the xml.
   *
   * @return a collection of {@link Content}
   */
  List<Content> getContent();
}
