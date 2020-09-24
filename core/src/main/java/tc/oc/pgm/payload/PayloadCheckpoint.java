package tc.oc.pgm.payload;

/** A wrapper for some info relevant to checkpoints on a Payload rail */
public class PayloadCheckpoint {

  private final int index;
  private final int mapIndex; // Self aware of their index in the map they are stored in
  private final boolean permanent;

  PayloadCheckpoint(int index, int mapIndex, boolean permanent) {
    this.index = index;
    this.mapIndex = mapIndex;
    this.permanent = permanent;
  }

  public int index() {
    return index;
  }

  public int getMapIndex() {
    return mapIndex;
  }

  public boolean isPermanent() {
    return permanent;
  }

  public boolean isMiddle() {
    return mapIndex == 0;
  }
}
