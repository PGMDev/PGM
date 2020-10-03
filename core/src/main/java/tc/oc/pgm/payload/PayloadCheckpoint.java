package tc.oc.pgm.payload;

import javax.annotation.Nullable;
import org.bukkit.util.Vector;

/**
 * A wrapper for some info relevant to checkpoints on a Payload rail initially constructed when
 * parsing the XML, index and mapIndex given after the payload rail is constructed
 */
public class PayloadCheckpoint {

  @Nullable private final String id; // Optionally for checkpoint filters
  private final Vector location;
  private int index;
  private int mapIndex; // Self aware of their index in the map they are stored in
  private final boolean permanent;

  PayloadCheckpoint(@Nullable String id, Vector location, boolean permanent) {
    this.id = id;
    this.location = location;
    this.permanent = permanent;
  }

  PayloadCheckpoint(
      @Nullable String id, Vector location, boolean permanent, int index, int mapIndex) {
    this.id = id;
    this.location = location;
    this.permanent = permanent;
    this.index = index;
    this.mapIndex = mapIndex;
  }

  public Vector getLocation() {
    return location;
  }

  @Nullable
  public String getId() {
    return id;
  }

  public int index() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getMapIndex() {
    return mapIndex;
  }

  public void setMapIndex(int mapIndex) {
    this.mapIndex = mapIndex;
  }

  public boolean isPermanent() {
    return permanent;
  }

  public boolean isMiddle() {
    return mapIndex == 0;
  }
}
