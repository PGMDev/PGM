package tc.oc.pgm.map.source;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.util.FileUtils;
import tc.oc.util.logging.ClassLogger;

public class FileMapSourceFactory implements MapSourceFactory {

  private final Logger logger;
  private final File dir;
  private final Map<String, MapSource> sources;

  public FileMapSourceFactory(Logger logger, File dir) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass(), dir.toString());
    this.dir = checkNotNull(dir).getAbsoluteFile();
    this.sources = Collections.synchronizedMap(new WeakHashMap<>());
  }

  private boolean shouldReloadSource(@Nullable MapSource source) {
    try {
      return source != null && source.checkForUpdates();
    } catch (MapNotFoundException e) {
      logger.log(Level.WARNING, "Skipping map source " + source.getId(), e);
      sources.remove(source.getId());
    }
    return false;
  }

  private boolean shouldLoadNewSource(@Nullable Path path) {
    return path != null
        && !sources.containsKey(path.toString())
        && Files.isDirectory(path)
        && Files.isRegularFile(path.resolve(MapSource.FILE));
  }

  @Override
  public Iterator<MapSource> loadSources() throws MapNotFoundException {
    try {
      return Iterators.filter(
          Iterators.concat(
              sources.values().iterator(),
              Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS)
                  .filter(this::shouldLoadNewSource)
                  .map(FileMapSource::new)
                  .iterator()),
          this::shouldReloadSource);
    } catch (IOException e) {
      throw new MapNotFoundException(e);
    }
  }

  @Override
  public void reset() {
    sources.clear();
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
    return new ToStringBuilder(this).append("dir", dir).append("sources", sources.size()).build();
  }

  private static class FileMapSource implements MapSource {

    private final String path;
    private final AtomicLong modified;

    public FileMapSource(Path path) {
      this(path.toString());
    }

    public FileMapSource(String path) {
      this.path = checkNotNull(path);
      this.modified = new AtomicLong(0);
    }

    public File getFile() {
      return new File(path, MapSource.FILE).getAbsoluteFile();
    }

    @Override
    public String getId() {
      return path;
    }

    @Override
    public void downloadTo(File dir) throws IOException {
      FileUtils.copy(getFile().getParentFile(), checkNotNull(dir), true);
    }

    @Override
    public InputStream getDocument() throws FileNotFoundException {
      final File file = getFile();
      modified.set(file.lastModified());
      return new FileInputStream(file);
    }

    @Override
    public boolean checkForUpdates() throws MapNotFoundException {
      final File file = getFile();

      if (!file.exists()) {
        throw new MapNotFoundException(
            "Cannot find map source file", new FileNotFoundException(file.toString()));
      }

      if (!file.canRead()) {
        throw new MapNotFoundException(
            "Cannot read map source file (likely a file system permissions issue)",
            new FileNotFoundException(file.toString()));
      }

      return getFile().lastModified() > modified.get();
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
