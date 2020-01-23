package tc.oc.pgm.map.source;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;

// TODO: Instead of bundling a zip with the jar, implement a GithubMapSourceFactory and use that
public class DefaultMapSourceFactory extends SystemMapSourceFactory {

  private final AtomicBoolean downloaded;

  public DefaultMapSourceFactory() {
    super("maps/");
    this.downloaded = new AtomicBoolean();
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    if (!downloaded.get()) downloadSources();
    return super.loadNewSources();
  }

  @Override
  public void reset() {
    super.reset();
    downloaded.compareAndSet(true, false);
  }

  private void downloadSources() throws MapMissingException {
    final byte[] buffer = new byte[1024];
    try (final ZipInputStream zip = new ZipInputStream(PGM.get().getResource("maps.zip"))) {
      ZipEntry entry = zip.getNextEntry();
      while (entry != null) {
        final File dst = new File(dir, entry.getName());
        final File dst1 = dst.getParentFile();
        if (!entry.isDirectory() && !dst.exists() && (dst1.exists() || dst1.mkdirs())) {
          try (final FileOutputStream output = new FileOutputStream(dst)) {
            int len;
            while ((len = zip.read(buffer)) > 0) {
              output.write(buffer, 0, len);
            }
          }
        }
        entry = zip.getNextEntry();
      }
    } catch (IOException e) {
      throw new MapMissingException(dir.getPath(), "Could not unzip default maps", e);
    } finally {
      downloaded.compareAndSet(false, true);
    }
  }
}
