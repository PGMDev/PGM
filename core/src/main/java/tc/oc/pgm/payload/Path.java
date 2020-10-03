package tc.oc.pgm.payload;

import org.bukkit.Location;

public class Path {
  private final int index;
  private final Location location;
  private Path previousPath;
  private Path nextPath;
  private final boolean checkpoint;

  Path(Location location) {
    this(0, location, null, null, false);
  }

  Path(Path oldPath, int nowWithIndex) {
    this(
        nowWithIndex,
        oldPath.getLocation(),
        oldPath.previous(),
        oldPath.next(),
        oldPath.isCheckpoint());
  }

  Path(int index, Location location, Path previousPath, Path nextPath, boolean checkpoint) {
    this.index = index;
    this.location = location;
    this.previousPath = previousPath;
    this.nextPath = nextPath;
    this.checkpoint = checkpoint;
  }

  public int index() {
    return index;
  }

  public Location getLocation() {
    return location;
  }

  public boolean hasPrevious() {
    return previous() != null;
  }

  public Path previous() {
    return previousPath;
  }

  public void setPrevious(Path previousPath) {
    this.previousPath = previousPath;
  }

  public boolean hasNext() {
    return next() != null;
  }

  public Path next() {
    return nextPath;
  }

  public void setNext(Path nextPath) {
    this.nextPath = nextPath;
  }

  public boolean isCheckpoint() {
    return checkpoint;
  }
}
