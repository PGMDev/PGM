package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class MapSource implements Comparable<MapSource> {

  private final @Nullable URL url;
  private final Path path;
  private final int maxDepth;
  private final Set<Path> onlyPaths;
  private final Set<Path> excludedPaths;
  private final int priority; // Lowest priority source wins a map name conflict

  public MapSource(
      Path path,
      @Nullable URL url,
      int maxDepth,
      Set<Path> onlyPaths,
      Set<Path> excludedPaths,
      int priority) {
    checkArgument(path.isAbsolute());

    this.url = url;
    this.path = checkNotNull(path);
    this.maxDepth = maxDepth;
    this.onlyPaths = checkNotNull(onlyPaths);
    this.excludedPaths = checkNotNull(excludedPaths);
    this.priority = priority;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + getPath().toString() + "}";
  }

  @Override
  public int compareTo(MapSource o) {
    return Integer.compare(priority, o.priority);
  }

  public boolean hasPriorityOver(MapSource o) {
    return compareTo(o) < 0;
  }

  public Path getPath() {
    return path;
  }

  public @Nullable URL getUrl() {
    return url;
  }

  protected Set<Path> getRootPaths() {
    if (onlyPaths.isEmpty()) {
      return Collections.singleton(Paths.get(""));
    } else {
      return onlyPaths;
    }
  }

  protected boolean isExcluded(Path path) {
    path = getPath().relativize(path);
    for (Path excludedPath : excludedPaths) {
      if (path.startsWith(excludedPath)) return true;
    }
    return false;
  }

  public Set<Path> getMapFolders(final Logger logger) throws IOException {
    final Set<Path> mapFolders = new HashSet<>();
    for (Path root : getRootPaths()) {
      int depth = "".equals(root.toString()) ? 0 : Iterables.size(root);
      Files.walkFileTree(
          getPath().resolve(root),
          ImmutableSet.of(FileVisitOption.FOLLOW_LINKS),
          maxDepth - depth,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              if (!isExcluded(dir)) {
                if (MapFolder.isMapFolder(dir)) {
                  mapFolders.add(dir);
                }
                return FileVisitResult.CONTINUE;
              } else {
                logger.fine("Skipping excluded path " + dir);
                return FileVisitResult.SKIP_SUBTREE;
              }
            }
          });
    }
    return mapFolders;
  }
}
