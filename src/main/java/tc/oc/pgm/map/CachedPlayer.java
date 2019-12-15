package tc.oc.pgm.map;

import java.util.UUID;

public class CachedPlayer {

  private final UUID uuid;
  private String name;
  private long timestamp;

  public CachedPlayer(UUID uuid, String name, long timestamp) {
    this.uuid = uuid;
    this.name = name;
    this.timestamp = timestamp;
  }

  public UUID getUUID() {
    return uuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
