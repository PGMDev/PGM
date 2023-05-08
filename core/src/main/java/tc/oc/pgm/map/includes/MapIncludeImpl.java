package tc.oc.pgm.map.includes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;

public class MapIncludeImpl implements MapInclude {

  private final File file;
  private final AtomicLong lastRead;
  private Document source;

  public MapIncludeImpl(File file) throws MapMissingException, JDOMException, IOException {
    this.file = file;
    this.lastRead = new AtomicLong(-1);
    reload();
  }

  private void reload() throws MapMissingException, JDOMException, IOException {
    try (InputStream is = new FileInputStream(file)) {
      this.source = MapIncludeProcessorImpl.DOCUMENT_FACTORY.get().build(is);
    } catch (FileNotFoundException e) {
      throw new MapMissingException(file.getPath(), "Unable to read map include document", e);
    } finally {
      lastRead.set(System.currentTimeMillis());
    }
  }

  @Override
  public List<Content> getContent() {
    if (getLastModified() > lastRead.get()) {
      try {
        reload();
      } catch (MapMissingException | JDOMException | IOException e) {
        PGM.get()
            .getGameLogger()
            .log(Level.SEVERE, "Failed to reload modified include document " + file.getName(), e);
      }
    }
    return source.getRootElement().cloneContent();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MapIncludeImpl && file.equals(((MapIncludeImpl) other).file);
  }

  @Override
  public long getLastModified() {
    return file.lastModified();
  }
}
