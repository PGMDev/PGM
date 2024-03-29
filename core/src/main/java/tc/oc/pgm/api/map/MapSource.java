package tc.oc.pgm.api.map;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.map.source.MapRoot;

/** A source where {@link MapInfo} documents and files are downloaded. */
public interface MapSource {
  Path FILE = Paths.get("map.xml");
  String DEFAULT_VARIANT = "default";

  /**
   * Get a unique identifier for the source, should be human-readable.
   *
   * @return A unique identifier.
   */
  String getId();

  /**
   * The variant of the map this is for
   *
   * @return the variant the source, DEFAULT_VARIANT for the parent source
   */
  String getVariantId();

  /**
   * A copy of the map source, tailored to a specific variant
   *
   * @param variant variant to use
   * @return a new instance of map source, using the variant
   */
  MapSource asVariant(String variant);

  /**
   * Download the {@link org.bukkit.World} files to a local directory.
   *
   * @param folder subfolder to download, null for parent
   * @param dir An existent, but empty directory.
   * @throws MapMissingException If an error occurs while creating the files.
   */
  void downloadTo(String folder, File dir) throws MapMissingException;

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

  /**
   * Sets the collection of includes the map source references
   *
   * @param include The {@link MapInclude}
   */
  void setIncludes(Collection<MapInclude> include);

  MapRoot getRoot();

  Path getRelativeDir();

  default Path getRelativeXml() {
    return getRelativeDir().resolve(FILE);
  }

  default Path getAbsoluteDir() {
    return getRoot().getBase().resolve(getRelativeDir());
  }

  default Path getAbsoluteXml() {
    return getAbsoluteDir().resolve(FILE);
  }
}
