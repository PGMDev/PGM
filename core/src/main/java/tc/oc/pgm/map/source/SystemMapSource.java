package tc.oc.pgm.map.source;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;
import tc.oc.pgm.util.FileUtils;

class SystemMapSource implements MapSource {

  private final Path dir;
  private final String variant;
  private final AtomicLong lastRead;
  private final Set<MapInclude> storedIncludes;

  public SystemMapSource(Path dir, @Nullable String variant) {
    this.dir = assertNotNull(dir);
    this.variant = variant;
    this.lastRead = new AtomicLong(-1);
    this.storedIncludes = Sets.newHashSet();
  }

  private File getDirectory() throws MapMissingException {
    final File dir = this.dir.toFile();

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
    final File file = dir.resolve(MapSource.FILE).toFile();

    if (!file.exists()) {
      throw new MapMissingException(file.getPath(), "Unable to find map document (was it moved?)");
    }

    if (!file.isFile()) {
      throw new MapMissingException(file.getPath(), "Unable to read map document (is it a file?)");
    }

    if (!file.canRead()) {
      throw new MapMissingException(
          file.getPath(), "Unable to read map document (file permissions issue?)");
    }

    return file;
  }

  @Override
  public String getId() {
    return dir.toString();
  }

  @Override
  public void downloadTo(File dst) throws MapMissingException {
    final File src = getDirectory();
    try {
      FileUtils.copy(src, dst, true);
    } catch (IOException e) {
      throw new MapMissingException(dir.toString(), "Unable to copy map folder", e);
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
  public String getVariant() {
    return variant;
  }

  @Override
  public MapSource asVariant(String variant) {
    if (Objects.equals(variant, this.variant)) return this;
    return new SystemMapSource(dir, variant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dir, variant);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SystemMapSource)) return false;
    SystemMapSource other = (SystemMapSource) obj;
    return dir.equals(other.dir) && Objects.equals(variant, other.variant);
  }

  @Override
  public String toString() {
    return "SystemMapSource{dir="
        + this.dir
        + ", variant="
        + this.variant
        + ", lastRead="
        + this.lastRead.get()
        + "}";
  }

  @Override
  public void setIncludes(Collection<MapInclude> includes) {
    this.storedIncludes.clear();
    this.storedIncludes.addAll(includes);
  }
}
