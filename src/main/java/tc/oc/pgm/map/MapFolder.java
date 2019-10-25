package tc.oc.pgm.map;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

public class MapFolder {

  public static final String MAP_DESCRIPTION_FILE_NAME = "map.xml";
  public static final String THUMBNAIL_FILE_NAME = "map.png";

  public static boolean isMapFolder(Path path) {
    return Files.isDirectory(path) && Files.isRegularFile(path.resolve(MAP_DESCRIPTION_FILE_NAME));
  }

  private final MapSource source;
  private final Path path;
  private Collection<String> thumbnails;

  public MapFolder(MapSource source, Path path) {
    this.source = source;
    this.path = path;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + getAbsolutePath().toString() + "}";
  }

  public MapSource getSource() {
    return source;
  }

  public Path getAbsolutePath() {
    return source.getPath().resolve(path);
  }

  public Path getRelativePath() {
    return source.getPath().relativize(path);
  }

  public Path getAbsoluteDescriptionFilePath() {
    return getAbsolutePath().resolve(MAP_DESCRIPTION_FILE_NAME);
  }

  public Path getRelativeDescriptionFilePath() {
    return getRelativePath().resolve(MAP_DESCRIPTION_FILE_NAME);
  }

  private @Nullable URL getRelativeUrl(Path path) {
    // Resolving a Path against a URL is surprisingly tricky, due to character escaping issues.
    // The safest approach seems to be appending the path components one at a time, wrapping
    // each one in a URI to ensure that the filename is properly escaped. Trying to append the
    // entire thing at once either fails to escape illegal chars at all, or escapes characters
    // that shouldn't be, like the path seperator.
    try {
      URL url = source.getUrl();
      if (url == null) return null;

      URI uri = url.toURI();

      if (uri.getPath() == null || "".equals(uri.getPath())) {
        uri = uri.resolve("/");
      }

      Path dir = Files.isDirectory(source.getPath().resolve(path)) ? path : path.getParent();
      for (Path part : dir) {
        uri = uri.resolve(new URI(null, null, part.toString() + "/", null));
      }
      if (path != dir) {
        uri = uri.resolve(new URI(null, null, path.getFileName().toString(), null));
      }

      return uri.toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      return null;
    }
  }

  public @Nullable URL getUrl() {
    return getRelativeUrl(getRelativePath());
  }

  public @Nullable URL getDescriptionFileUrl() {
    return getRelativeUrl(getRelativeDescriptionFilePath());
  }

  public Collection<String> getThumbnails() {
    if (thumbnails == null) {
      if (Files.isRegularFile(getAbsolutePath().resolve(THUMBNAIL_FILE_NAME))) {
        thumbnails = Collections.singleton(THUMBNAIL_FILE_NAME);
      } else {
        thumbnails = Collections.emptySet();
      }
    }
    return thumbnails;
  }
}
