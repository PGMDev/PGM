package tc.oc.pgm.api.map;

import java.io.File;
import java.io.InputStream;
import tc.oc.pgm.api.map.exception.MapMissingException;

/** A source where {@link MapInfo} documents and files are downloaded. */
public interface MapSource {
  String FILE = "map.xml";

  /**
   * Get a unique identifier for the source, should be human-readable.
   *
   * @return A unique identifier.
   */
  String getId();

  /**
   * Download the {@link org.bukkit.World} files to a local directory.
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
   * Get whether future calls to {@link #getDocument()} will return different results.
   *
   * @return If there are available updates, resulting in a different document.
   * @throws MapMissingException If the document can no longer be found.
   */
  boolean checkForUpdates() throws MapMissingException;
}
