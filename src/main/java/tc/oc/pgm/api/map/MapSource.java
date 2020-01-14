package tc.oc.pgm.api.map;

import tc.oc.pgm.api.map.exception.MapNotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** A source where {@link MapContext} documents and files are downloaded. */
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
   * @throws IOException If an error occurs while creating the files.
   */
  void downloadTo(File dir) throws IOException;

  /**
   * Get an {@link InputStream} of the map's xml document.
   *
   * @return An xml document stream.
   */
  InputStream getDocument() throws IOException;

  /**
   * Get whether any subsequent calls to {@link #getDocument()} will result in a different {@link
   * InputStream}.
   *
   * @return If there are available updates, resulting in a different document.
   * @throws MapNotFoundException If the document can no longer be found.
   */
  boolean checkForUpdates() throws MapNotFoundException;
}
