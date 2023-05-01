package tc.oc.pgm.api.map;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import org.bukkit.World;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;

/**
 * A directory where map documents and files are downloaded from. Methods might error if the source
 * moves or changes after this object is created. (e.g a file moves to a new directory)
 *
 * <p>A valid source is a directory containing at least:
 *
 * <ul>
 *   <li>An XML Document following the PGM spec(documented at <a href="https://pgm.dev">the PGM
 *       website)</a>
 *   <li>Files for generating a {@link World}
 * </ul>
 *
 * @see MapMissingException
 * @see MapContext
 */
public interface MapSource {
  String FILE = "map.xml";

  /**
   * Get a unique identifier for the source, should be human-readable.
   *
   * @return A unique identifier.
   */
  String getId();

  /**
   * Download the {@link World} files to a local directory.
   *
   * @param dir An existent, but empty directory.
   * @throws MapMissingException If an error occurs while creating the files.
   */
  void downloadTo(File dir) throws MapMissingException;

  /**
   * Get an {@link InputStream} of the map's xml document.
   *
   * @throws MapMissingException If an error occurs while reading the document.
   * @return An xml document stream.
   */
  InputStream getDocument() throws MapMissingException;

  /**
   * Get the location of the source backing this {@link MapSource} in the form of an {@link URI}.
   *
   * @throws MapMissingException If an error occurs while attempting to look up the location of the
   *     source
   */
  URI getURI() throws MapMissingException;

  /**
   * Get whether future calls to {@link #getDocument()} will return different results.
   *
   * @return If there are available updates, resulting in a different document.
   * @throws MapMissingException If the document can no longer be found.
   */
  boolean checkForUpdates() throws MapMissingException;

  /**
   * Sets the collection of includes the map source references
   *
   * @param include The {@link MapInclude}
   */
  void setIncludes(Collection<MapInclude> include);
}
