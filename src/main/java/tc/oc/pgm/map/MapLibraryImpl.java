package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.util.SemanticVersion;
import tc.oc.util.StringUtils;
import tc.oc.util.logging.ClassLogger;

public class MapLibraryImpl implements MapLibrary {

  private final Logger logger;
  private final Set<MapSourceFactory> sources;
  private final Map<String, MapContext> maps;

  public MapLibraryImpl(Logger logger, Collection<MapSourceFactory> sources) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.sources = Collections.synchronizedSet(new HashSet<>(checkNotNull(sources)));
    this.maps = Collections.synchronizedSortedMap(new TreeMap<>());
  }

  @Override
  public MapContext getMap(String idOrName) {
    idOrName = MapInfoImpl.normalizeName(idOrName);

    final MapContext map = maps.get(idOrName);
    if (map == null) {
      return StringUtils.bestFuzzyMatch(idOrName, maps, 0.75);
    }

    return map;
  }

  @Override
  public Collection<MapContext> getMaps() {
    return ImmutableList.copyOf(maps.values());
  }

  @Override
  public CompletableFuture<Boolean> loadMaps(boolean force) {
    final CountDownLatch lock = new CountDownLatch(1);
    final Iterator<MapSourceFactory> iterator = sources.iterator();

    while (iterator.hasNext()) {
      final MapSourceFactory factory = iterator.next();

      if (force) {
        factory.reset();
      }

      if (!addFactory(factory, lock)) {
        iterator.remove();
      }
    }

    // FIXME: Must run this after all maps loaded, since async it's not timed properly right now
    UsernameResolver.resolveAll();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            lock.await(); // Waits for at least 1 map to load
            return true;
          } catch (InterruptedException e) {
            return false;
          }
        });
  }

  @Override
  public SemanticVersion getProto() {
    return ProtoVersions.FILTER_FEATURES;
  }

  private void addSource(MapSource source, CountDownLatch lock) {
    final MapContext map = new MapContextImpl(logger, source);
    try {
      map.load();
      lock.countDown(); // Notifies that at least 1 map has been loaded
    } catch (ModuleLoadException e) {
      logger.log(Level.WARNING, "Skipping map context: " + map, e);
      return;
    }

    maps.merge(
        map.getInfo().getId(),
        map,
        (MapContext old, MapContext now) -> {
          old.unload();
          return now;
        });
  }

  private boolean addFactory(MapSourceFactory factory, CountDownLatch lock) {
    final Iterator<MapSource> sources;
    try {
      sources = factory.loadSources();
    } catch (MapNotFoundException e) {
      logger.log(Level.WARNING, "Skipping map source factory: " + factory, e);
      return false;
    }

    sources.forEachRemaining(source -> CompletableFuture.runAsync(() -> addSource(source, lock)));
    return true;
  }
}
