package tc.oc.pgm.map;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.map.source.StreamMapSourceFactory;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.UsernameResolver;

public class MapLibraryImpl implements MapLibrary {

  private final Logger logger;
  private final List<MapSourceFactory> factories;
  private final SortedMap<String, MapInfo> maps;
  private final Set<MapSource> failed;
  private final MapIncludeProcessor includes;

  public MapLibraryImpl(
      Logger logger, List<MapSourceFactory> factories, MapIncludeProcessor includes) {
    this.logger = assertNotNull(logger); // Logger should be visible in-game
    this.factories = Collections.synchronizedList(assertNotNull(factories));
    this.maps = Collections.synchronizedSortedMap(new ConcurrentSkipListMap<>());
    this.failed = Collections.synchronizedSet(new HashSet<>());
    this.includes = includes;
  }

  @Override
  public MapInfo getMap(String idOrName) {

    // Exact match
    MapInfo map = maps.get(StringUtils.slugify(idOrName));
    if (map == null) {
      // Fuzzy match
      map =
          StringUtils.bestFuzzyMatch(
              StringUtils.normalize(idOrName), maps.values(), MapInfo::getNormalizedName);
    }

    return map;
  }

  @Override
  public Stream<MapInfo> getMaps(@Nullable String query) {
    Stream<MapInfo> maps = this.maps.values().stream();
    if (query != null) {
      String normalized = StringUtils.normalize(query);
      maps = maps.filter(mi -> LiquidMetal.match(mi.getNormalizedName(), normalized));
    }
    return maps;
  }

  @Override
  public Iterator<MapInfo> getMaps() {
    return getMaps(null).iterator();
  }

  @Override
  public long getSize() {
    return maps.size();
  }

  @Override
  public MapIncludeProcessor getIncludeProcessor() {
    return includes;
  }

  private void logMapError(MapException err) {
    logger.log(Level.WARNING, err.getMessage(), err);
  }

  private void logMapSuccess(int fail, int ok) {
    fail = failed.size() - fail;
    ok = maps.size() - ok;

    if (fail <= 0 && ok <= 0) {
      logger.info("No new maps found");
    } else if (fail <= 0) {
      logger.info("Loaded " + ChatColor.YELLOW + ok + ChatColor.RESET + " new maps");
    } else if (ok <= 0) {
      logger.info("Failed to load " + ChatColor.YELLOW + fail + ChatColor.RESET + " maps");
    } else {
      logger.info(
          "Loaded "
              + ChatColor.YELLOW
              + ok
              + ChatColor.RESET
              + " new maps, failed to load "
              + ChatColor.YELLOW
              + fail
              + ChatColor.RESET
              + " maps");
    }
  }

  @Override
  public CompletableFuture<?> loadNewMaps(boolean reset) {
    // Try to search new includes before searching for new maps
    includes.loadNewIncludes();

    final List<MapSourceFactory> sources = new LinkedList<>();

    // Reload failed maps
    if (reset) {
      failed.clear();
    } else {
      sources.add(new StreamMapSourceFactory(failed.stream()));
    }

    final int oldFail = failed.size();
    final int oldOk = reset ? 0 : maps.size();

    // Discover new maps
    for (MapSourceFactory factory : this.factories) {
      if (reset) factory.reset();
      sources.add(factory);
    }

    // Reload or delete existing maps that have updates
    sources.add(
        new StreamMapSourceFactory(
            this.maps.entrySet().stream()
                .filter(
                    entry -> {
                      try {
                        return reset || entry.getValue().getSource().checkForUpdates();
                      } catch (MapMissingException e) {
                        logMapError(e);
                        this.maps.remove(entry.getKey());
                        return false;
                      }
                    })
                .map(entry -> entry.getValue().getSource())));

    return CompletableFuture.runAsync(
            () -> {
              try (Stream<? extends MapSource> stream =
                  sources.stream()
                      .flatMap(s -> s.loadNewSources(this::logMapError))
                      .parallel()
                      .unordered()) {
                stream.forEach(s -> this.loadMapSafe(s, null));
              }
            })
        .thenRunAsync(() -> logMapSuccess(oldFail, oldOk))
        .thenRunAsync(UsernameResolver::resolveAll);
  }

  @Override
  public CompletableFuture<MapContext> loadExistingMap(String id) {
    return CompletableFuture.supplyAsync(
        () -> {
          final MapInfo info = maps.get(id);
          if (info == null) {
            throw new RuntimeException(
                new MapMissingException(id, "Unable to find map from id (was it deleted?)"));
          }

          final MapContext context = info.getContext();
          try {
            if (context != null && !info.getSource().checkForUpdates()) {
              return context;
            }
          } catch (MapMissingException e) {
            failed.remove(info.getSource());
            maps.remove(id);
            throw new RuntimeException(e);
          }

          logger.info(ChatColor.GREEN + "XML changes detected, reloading");
          return loadMapSafe(info.getSource(), info.getId());
        });
  }

  private MapContext loadMap(MapSource source, @Nullable String mapId) throws MapException {
    final MapContext context;
    try (final MapFactory factory = new MapFactoryImpl(logger, source, includes)) {
      context = factory.load();

      // We're not loading a specific map id, and we're not on a variant, load variants
      if (mapId == null && source.getVariant() == null) {
        for (String variant : factory.getVariants()) {
          loadMapSafe(source.asVariant(variant), null);
        }
      }

    } catch (MapMissingException e) {
      failed.remove(source);
      if (mapId != null) maps.remove(mapId);
      throw e;
    } catch (MapException e) {
      failed.add(source);
      throw e;
    } catch (Throwable t) {
      throw new MapException(
          source,
          null,
          "Unhandled " + t.getClass().getName() + ": " + t.getMessage(),
          t.getCause());
    }

    MapInfo info = context.getInfo();
    maps.merge(
        info.getId(), info, (m1, m2) -> m2.getVersion().isOlderThan(m1.getVersion()) ? m1 : m2);
    failed.remove(source);

    return context;
  }

  private @Nullable MapContext loadMapSafe(MapSource source, @Nullable String mapId) {
    try {
      return loadMap(source, mapId);
    } catch (MapException e) {
      logMapError(e);
    }
    return null;
  }
}
