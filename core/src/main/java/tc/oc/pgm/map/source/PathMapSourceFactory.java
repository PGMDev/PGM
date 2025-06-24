package tc.oc.pgm.map.source;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;

public class PathMapSourceFactory implements MapSourceFactory {

  private final Set<Path> sources;
  protected MapRoot paths;

  protected final @Nullable List<Path> children;

  public PathMapSourceFactory(Path base) {
    this(new MapRoot(base), null);
  }

  public PathMapSourceFactory(MapRoot base, @Nullable List<Path> children) {
    this.sources = Collections.synchronizedSet(new ConcurrentSkipListSet<>());
    this.paths = assertNotNull(base);
    this.children = children;
  }

  protected MapSource loadSource(Path path) {
    return new SystemMapSource(paths, path, null);
  }

  private Stream<MapSource> loadNewSource(Path path) {
    if (path == null || !path.startsWith(paths.getBase()) || !sources.add(path)) {
      return Stream.empty();
    }

    if (path.getFileName().equals(MapSource.FILE)) {
      return Stream.of(loadSource(paths.getBase().relativize(path.getParent())));
    }
    return Stream.empty();
  }

  @Override
  public Stream<MapSource> loadNewSources(Consumer<MapException> exceptionHandler) {
    if (!Files.exists(paths.getBase()) || !Files.isDirectory(paths.getBase()))
      return Stream.empty();

    return (children == null
            ? Stream.of(paths.getBase())
            : children.stream().map(paths.getBase()::resolve))
        .flatMap(
            b -> {
              try {
                return Files.walk(b, FileVisitOption.FOLLOW_LINKS);
              } catch (IOException e) {
                exceptionHandler.accept(
                    new MapMissingException(b.toString(), "Unable to list files", e));
                return Stream.empty();
              }
            })
        .map(Path::toAbsolutePath)
        .parallel()
        .flatMap(this::loadNewSource);
  }

  @Override
  public void reset() {
    sources.clear();
  }
}
