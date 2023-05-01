package tc.oc.pgm.map.source;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.util.FileUtils;

public class SystemMapSourceFactory extends PathMapSourceFactory {

  protected final File dir;

  public SystemMapSourceFactory(File file) {
    super(file.getPath());
    this.dir = file;
  }

  @Override
  protected MapSource loadSource(String dir) {
    return new SystemMapSource(dir);
  }

  @Override
  protected Stream<String> loadAllPaths() throws IOException {
    if (!dir.exists() || !dir.isDirectory()) return Stream.empty();

    return Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS)
        .map(Path::toAbsolutePath)
        .map(Path::toString);
  }

  protected static class SystemMapSource implements MapSource {

    private final String dir;
    private final AtomicLong lastRead;
    private final Set<MapInclude> storedIncludes;

    private SystemMapSource(String dir) {
      this.dir = assertNotNull(dir);
      this.lastRead = new AtomicLong(-1);
      this.storedIncludes = Sets.newHashSet();
    }

    private File getDirectory() throws MapMissingException {
      final File dir = new File(this.dir);

      if (!dir.exists()) {
        throw new MapMissingException(dir.getPath(), "Unable to find map folder (was it moved?)");
      }

      if (!dir.isDirectory()) {
        throw new MapMissingException(
            dir.getPath(), "Unable to read map folder (is it a directory?)");
      }

      return dir;
    }

    private File getFile() throws MapMissingException {
      final File file = new File(dir, MapSource.FILE);

      if (!file.exists()) {
        throw new MapMissingException(
            file.getPath(), "Unable to find map document (was it moved?)");
      }

      if (!file.isFile()) {
        throw new MapMissingException(
            file.getPath(), "Unable to read map document (is it a file?)");
      }

      if (!file.canRead()) {
        throw new MapMissingException(
            file.getPath(), "Unable to read map document (file permissions issue?)");
      }

      return file;
    }

    @Override
    public String getId() {
      return dir;
    }

    @Override
    public void downloadTo(File dst) throws MapMissingException {
      final File src = getDirectory();
      try {
        FileUtils.copy(src, dst, true);
      } catch (IOException e) {
        throw new MapMissingException(dir, "Unable to copy map folder", e);
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
        lastRead.set(System.currentTimeMillis());
      }
    }

    @Override
    public URI getURI() throws MapMissingException {
      final File file = getFile();

      return file.toURI();
    }

    @Override
    public boolean checkForUpdates() throws MapMissingException {
      final File file = getFile();

      final long read = lastRead.get();
      if (read <= 0) return false;
      if (file.lastModified() > read) return true;

      for (MapInclude include : storedIncludes) {
        if (include.getLastModified() > read) return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return dir.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SystemMapSource)) return false;
      return dir.equals(((SystemMapSource) obj).dir);
    }

    @Override
    public String toString() {
      return "SystemMapSourceFactory{dir=" + this.dir + ", lastRead=" + this.lastRead.get() + "}";
    }

    @Override
    public void setIncludes(Collection<MapInclude> includes) {
      this.storedIncludes.clear();
      this.storedIncludes.addAll(includes);
    }
  }
}
