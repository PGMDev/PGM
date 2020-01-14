package tc.oc.pgm.map;

import com.google.common.collect.Iterators;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.util.SemanticVersion;
import tc.oc.util.StringUtils;
import tc.oc.util.logging.ClassLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapLibraryImpl implements MapLibrary {

  private final Logger logger;
  private final Set<MapSourceFactory> factories;
  private final Map<String, MapContext> loaded;
  private final Set<MapSource> failed;

  // FIXME: all maps are loaded and kept, must off-load some and reload
  public MapLibraryImpl(Logger logger, Collection<MapSourceFactory> factories) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.factories = Collections.synchronizedSet(new HashSet<>(checkNotNull(factories)));
    this.loaded = Collections.synchronizedMap(new TreeMap<>());
    this.failed = Collections.synchronizedSet(new HashSet<>());
  }

  @Override
  public SemanticVersion getProto() {
    return ProtoVersions.FILTER_FEATURES;
  }

  @Override
  public MapContext getMap(String idOrName) {
    idOrName = MapInfoImpl.normalizeName(idOrName);

    final MapContext map = loaded.get(idOrName);
    if (map == null) {
      return StringUtils.bestFuzzyMatch(idOrName, loaded, 0.75);
    }

    return map;
  }

  @Override
  public Collection<MapContext> getMaps() {
    return Collections.unmodifiableCollection(loaded.values());
  }

  @Override
  public CompletableFuture<?> loadNewMaps(boolean reset) {
    final List<Iterator<? extends MapSource>> sources = new LinkedList<>();

    if (reset) {
      failed.clear();
    } else {
      sources.add(failed.iterator());
    }

    final Iterator<MapSourceFactory> factories = this.factories.iterator();
    while (factories.hasNext()) {
      final MapSourceFactory factory = factories.next();
      try {
        sources.add(factory.loadNewSources());
      } catch (MapNotFoundException e) {
        logger.log(Level.WARNING, "Skipped source: " + factory, e);
        factories.remove();
      }
    }

    return CompletableFuture.runAsync(
        () -> Iterators.concat(sources.iterator()).forEachRemaining(this::addSource));
  }

  private void addSource(MapSource source) {
    if (source == null) return;
    MapFactory factory = null;

    try {
      factory = new MapFactoryImpl(logger, source);
      factory.load(); // TODO: lazily load maps based on whether they are set next
    } catch (ModuleLoadException e) {
      failed.add(source);
      logger.log(Level.WARNING, "Skipped map: " + source.getId(), e);
      return;
    } catch (MapNotFoundException e) {
      failed.remove(source);
      logger.log(Level.WARNING, "Missing map: " + source.getId(), e);
      return;
    }

    final MapContext context = new MapContextImpl(factory.getInfo(), source, factory);
    loaded.merge(
        context.getId(),
        context,
        (MapContext old, MapContext now) -> {
          logger.log(Level.INFO, "Replaced map: " + now.getId());
          return now;
        });
    failed.remove(source);
    logger.log(Level.INFO, "Loaded map: " + context.getId());
  }
}
