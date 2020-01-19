package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.util.StringUtils;
import tc.oc.util.logging.ClassLogger;

public class MapLibraryImpl implements MapLibrary {

  private final Logger logger;
  private final Set<MapSourceFactory> factories;
  private final SortedMap<String, MapEntry> maps;
  private final Set<MapSource> failed;

  private static class MapEntry {
    private final MapSource source;
    private final MapInfo info;
    private final WeakReference<MapContext> context;

    private MapEntry(MapSource source, MapInfo info, MapContext context) {
      this.source = checkNotNull(source);
      this.info = checkNotNull(info);
      this.context = new WeakReference<>(checkNotNull(context));
    }
  }

  public MapLibraryImpl(Logger logger, Collection<MapSourceFactory> factories) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.factories = Collections.synchronizedSet(new HashSet<>(checkNotNull(factories)));
    this.maps = Collections.synchronizedSortedMap(new TreeMap<>());
    this.failed = Collections.synchronizedSet(new HashSet<>());
  }

  @Override
  public MapInfo getMap(String idOrName) {
    idOrName = MapInfo.normalizeName(idOrName);

    MapEntry map = maps.get(idOrName);
    if (map == null) {
      map = StringUtils.bestFuzzyMatch(idOrName, maps, 0.75);
    }

    return map == null ? null : map.info;
  }

  @Override
  public Iterator<MapInfo> getMaps() {
    return maps.values().stream().map(entry -> entry.info).iterator();
  }

  @Override
  public CompletableFuture<?> loadNewMaps(boolean reset) {
    final List<Iterator<? extends MapSource>> sources = new LinkedList<>();

    // Reload failed maps
    if (reset) {
      failed.clear();
    } else {
      sources.add(failed.iterator());
    }

    // Discover new maps
    final Iterator<MapSourceFactory> factories = this.factories.iterator();
    while (factories.hasNext()) {
      final MapSourceFactory factory = factories.next();
      try {
        sources.add(factory.loadNewSources());
      } catch (MapNotFoundException e) {
        factories.remove();
        logger.log(Level.WARNING, "Skipped source: " + factory, e);
      }
    }

    // Reload existing maps that changed
    final Iterator<Map.Entry<String, MapEntry>> maps = this.maps.entrySet().iterator();
    while (maps.hasNext()) {
      final MapEntry entry = maps.next().getValue();
      try {
        if (entry.source.checkForUpdates()) {
          sources.add(Iterators.singletonIterator(entry.source));
        }
      } catch (MapNotFoundException e) {
        maps.remove();
        logger.log(Level.WARNING, "Missing map: " + entry.info.getId());
      }
    }

    return CompletableFuture.runAsync(
        () ->
            Iterators.concat(sources.iterator())
                .forEachRemaining(source -> loadMapSafe(source, null)));
  }

  @Override
  public CompletableFuture<MapContext> loadExistingMap(String id) {
    final MapEntry entry = maps.get(id);
    if (entry == null) {
      return CompletableFuture.supplyAsync(
          () -> {
            throw new MapNotFoundException("Could not find map: " + id);
          });
    }

    final MapContext context = entry.context.get();
    if (context != null) {
      return CompletableFuture.completedFuture(context);
    }

    return CompletableFuture.supplyAsync(() -> loadMap(entry.source, entry.info.getId()));
  }

  private MapContext loadMap(MapSource source, @Nullable String mapId)
      throws MapNotFoundException, ModuleLoadException {
    final MapFactory factory = new MapFactoryImpl(logger, source);
    final MapContext context;

    try {
      context = factory.load();
    } catch (ModuleLoadException e) {
      failed.add(source);
      throw e;
    } catch (MapNotFoundException e) {
      failed.remove(source);
      maps.remove(mapId);
      throw e;
    }

    maps.put(context.getId(), new MapEntry(source, context.clone(), context));
    return context;
  }

  private @Nullable MapContext loadMapSafe(MapSource source, @Nullable String mapId) {
    try {
      final MapContext context = loadMap(source, mapId);
      logger.log(Level.INFO, (mapId == null ? "Loaded map: " : "Replaced map: ") + context.getId());
      return context;
    } catch (MapNotFoundException e) {
      logger.log(Level.WARNING, "Skipped map: " + (mapId == null ? source.getId() : mapId), e);
    } catch (ModuleLoadException e) {
      logger.log(Level.WARNING, "Missing map: " + (mapId == null ? source.getId() : mapId), e);
    }
    return null;
  }

  @Override
  protected void finalize() throws Throwable {
    PGM.get().getLogger().info("Finalize: " + this);
    super.finalize();
  }
}
