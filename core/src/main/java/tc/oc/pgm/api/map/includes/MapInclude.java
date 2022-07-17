package tc.oc.pgm.api.map.includes;

import java.util.Collection;
import org.jdom2.Content;

/** Represents a snippet of XML that can be referenced for reuse * */
public interface MapInclude {

  /**
   * Get a unique id which identifies this MapInclude.
   *
   * @return A unique id
   */
  String getId();

  /**
   * Get the system file time from when this MapInclude file was last modified.
   *
   * @return Time of last file modification
   */
  long getLastModified();

  /**
   * Get a collection of {@link Content} which can be merged into an existing {@link Document}
   *
   * @return a collection of {@link Content}
   */
  Collection<Content> getContent();
}
