package tc.oc.pgm.payload;

import org.bukkit.Location;

public class Path {
  private final int index;
  private final Location location;
  private Path previousPath;
  private Path nextPath;
  private final boolean checkpoint;

  Path(Location location, Path previousPath, Path nextPath) {
    this(0, location, previousPath, nextPath, false);
  }

  Path(int index, Location location, Path previousPath, Path nextPath) {
    this(index, location, previousPath, nextPath, false);
  }

  Path(Location location, Path previousPath, Path nextPath, boolean checkpoint) {
    this(0, location, previousPath, nextPath, checkpoint);
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
