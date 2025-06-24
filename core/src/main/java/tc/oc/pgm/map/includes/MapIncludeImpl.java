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
import tc.oc.pgm.util.xml.DocumentWrapper;

public class MapIncludeImpl implements MapInclude {

  private final String id;
  private final File file;
  private final AtomicLong lastRead;
  private Document source;

  public MapIncludeImpl(String id, File file)
      throws MapMissingException, JDOMException, IOException {
    this.id = id;
    this.file = file;
    this.lastRead = new AtomicLong(-1);
    reload();
  }

  private void reload() throws MapMissingException, JDOMException, IOException {
    try (InputStream is = new FileInputStream(file)) {
      this.source = MapIncludeProcessorImpl.DOCUMENT_FACTORY.get().build(is);
      // Includes should never visit, let the map XML visit instead
      ((DocumentWrapper) this.source).setVisitingAllowed(false);
      this.source.setBaseURI("#" + id);
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
