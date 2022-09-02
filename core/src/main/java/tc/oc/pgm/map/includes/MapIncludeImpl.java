package tc.oc.pgm.map.includes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.includes.MapInclude;

public class MapIncludeImpl implements MapInclude {

  private final String id;
  private final Document source;
  private final AtomicLong lastModified;

  public MapIncludeImpl(File file) throws MapMissingException, JDOMException, IOException {
    try {
      InputStream fileStream = new FileInputStream(file);
      this.id = file.getName().replace(".xml", "");
      this.source = MapIncludeProcessorImpl.DOCUMENT_FACTORY.get().build(fileStream);
    } catch (FileNotFoundException e) {
      throw new MapMissingException(file.getPath(), "Unable to read map include document", e);
    } finally {
      lastModified = new AtomicLong(file.lastModified());
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Collection<Content> getContent() {
    return source.getRootElement().cloneContent();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof MapInclude)) return false;
    return ((MapInclude) other).getId().equalsIgnoreCase(getId());
  }

  @Override
  public long getLastModified() {
    return lastModified.get();
  }

  @Override
  public boolean hasBeenModified(long time) {
    return time > lastModified.get();
  }
}
