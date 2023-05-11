package tc.oc.pgm.map.source;

import java.util.function.Consumer;
import java.util.stream.Stream;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;

public class StreamMapSourceFactory implements MapSourceFactory {

  private final Stream<? extends MapSource> sources;

  public StreamMapSourceFactory(Stream<? extends MapSource> sources) {
    this.sources = sources;
  }

  @Override
  public Stream<? extends MapSource> loadNewSources(Consumer<MapException> exceptionHandler) {
    return sources;
  }

  @Override
  public void reset() {}
}
