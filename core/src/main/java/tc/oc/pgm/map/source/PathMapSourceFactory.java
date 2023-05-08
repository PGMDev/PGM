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

  protected final Path base;
  protected final @Nullable List<Path> children;

  public PathMapSourceFactory(Path base) {
    this(base, null);
  }

  public PathMapSourceFactory(Path base, @Nullable List<Path> children) {
    this.sources = Collections.synchronizedSet(new ConcurrentSkipListSet<>());
    this.base = assertNotNull(base).toAbsolutePath();
    this.children = children;
  }

  protected MapSource loadSource(Path path) {
    return new SystemMapSource(path, null);
  }

  private Stream<MapSource> loadNewSource(Path path) {
    if (path == null || !path.startsWith(this.base) || !sources.add(path)) {
      return Stream.empty();
    }

    if (path.getFileName().equals(MapSource.FILE)) {
      return Stream.of(loadSource(path.getParent()));
    }
    return Stream.empty();
  }

  @Override
  public Stream<? extends MapSource> loadNewSources(Consumer<MapException> exceptionHandler) {
    if (!Files.exists(base) || !Files.isDirectory(base)) return Stream.empty();

    return (children == null ? Stream.of(base) : children.stream().map(base::resolve))
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
