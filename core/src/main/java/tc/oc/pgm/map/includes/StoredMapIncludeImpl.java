package tc.oc.pgm.map.includes;

import java.util.concurrent.atomic.AtomicLong;
import tc.oc.pgm.api.map.includes.StoredMapInclude;

public class StoredMapIncludeImpl implements StoredMapInclude {

  private final String includeId;
  private final AtomicLong lastModified;

  public StoredMapIncludeImpl(String includeId, long lastModified) {
    this.includeId = includeId;
    this.lastModified = new AtomicLong(lastModified);
  }

  @Override
  public String getIncludeId() {
    return includeId;
  }

  @Override
  public boolean hasBeenModified(long time) {
    return time > lastModified.get();
  }
}
