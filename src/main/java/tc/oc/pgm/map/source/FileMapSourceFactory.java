package tc.oc.pgm.map.source;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.util.FileUtils;

public class FileMapSourceFactory implements MapSourceFactory {

  private final File dir;
  private final Set<String> seen;

  public FileMapSourceFactory(File dir) {
    this.dir = checkNotNull(dir).getAbsoluteFile();
    this.seen = new ConcurrentSkipListSet<>();
  }

  private boolean isSource(@Nullable Path path) {
    return path != null
        && !seen.contains(path.toString())
        && Files.isDirectory(path)
        && Files.isRegularFile(path.resolve(MapSource.FILE));
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    try {
      return Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS)
          .parallel()
          .filter(this::isSource)
          .map(Path::toString)
          .peek(seen::add)
          .map(FileMapSource::new)
          .iterator();
    } catch (IOException e) {
      throw new MapMissingException(dir.getPath(), "Unable to list map directories", e);
    }
  }

  @Override
  public void reset() {
    seen.clear();
  }

  @Override
  public int hashCode() {
    return dir.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FileMapSourceFactory)) return false;
    return dir.equals(((FileMapSourceFactory) obj).dir);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("dir", dir).append("seen", seen).build();
  }

  private static class FileMapSource implements MapSource {

    private final String path;
    private final AtomicLong modified;

    private FileMapSource(String path) {
      this.path = checkNotNull(path);
      this.modified = new AtomicLong(0);
    }

    private File getDirectory() {
      return new File(path).getAbsoluteFile();
    }

    private File getFile() {
      return new File(path, MapSource.FILE).getAbsoluteFile();
    }

    @Override
    public String getId() {
      return path;
    }

    @Override
    public void downloadTo(File dir) throws MapMissingException {
      final File src = getDirectory();
      try {
        FileUtils.copy(src, dir, true);
      } catch (IOException e) {
        throw new MapMissingException(src.getPath(), "Unable to copy map directory", e);
      }
    }

    @Override
    public InputStream getDocument() throws MapMissingException {
      final File file = getFile();
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new MapMissingException(file.getPath(), "Unable to read map document", e);
      } finally {
        modified.set(file.lastModified());
      }
    }

    @Override
    public boolean checkForUpdates() throws MapMissingException {
      final File file = getFile();

      if (!file.exists()) {
        throw new MapMissingException(
            file.getPath(), "Unable to locate the map document (was it moved?)");
      }

      if (!file.isFile()) {
        throw new MapMissingException(
            file.getPath(), "Unable to read map document because it is not a file");
      }

      if (!file.canRead()) {
        throw new MapMissingException(
            file.getPath(), "Unable to read map document (file permissions issue?)");
      }

      return file.lastModified() > modified.get();
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof FileMapSource)) return false;
      return path.equals(((FileMapSource) obj).path);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("path", path)
          .append("modified", modified.get())
          .build();
    }
  }
}
