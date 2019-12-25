package tc.oc.pgm.map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.util.logging.ClassLogger;

public class MapLibrary {

  protected final Map<String, PGMMap> mapsById = Maps.newHashMap();
  protected final Map<Path, PGMMap> mapsByPath = new HashMap<>();
  protected final SetMultimap<String, PGMMap> mapsByName = HashMultimap.create();
  protected final ClassLogger logger;

  public MapLibrary(Logger logger) {
    this.logger = ClassLogger.get(logger, getClass());
  }

  public Set<PGMMap> addMaps(Collection<PGMMap> maps) {
    Set<PGMMap> added = new HashSet<>();
    for (PGMMap map : maps) {
      if (addMap(map)) added.add(map);
    }
    return added;
  }

  public boolean addMap(PGMMap map) {
    final String id = map.getInfo().id;
    PGMMap old = mapsById.get(id);

    if (old == null) {
      logger.fine("Adding " + id);
    } else if (old.getSource().hasPriorityOver(map.getSource())) {
      logger.fine("Skipping duplicate " + id);
      return false;
    } else {
      logger.fine("Replacing duplicate " + id);
    }

    mapsById.put(id, map);
    mapsByPath.put(map.getFolder().getAbsolutePath(), map);
    mapsByName.put(map.getName(), map);
    return true;
  }

  public void removeMaps(Collection<Path> paths) {
    for (Path path : paths) removeMap(path);
  }

  public boolean removeMap(Path path) {
    PGMMap map = mapsByPath.remove(path);
    if (map == null) return false;

    mapsById.remove(map.getInfo().id);
    mapsByName.remove(map.getName(), map);
    return true;
  }

  public Logger getLogger() {
    return this.logger;
  }

  public Collection<PGMMap> getMaps() {
    return this.mapsById.values();
  }

  public Set<String> getMapNames() {
    return mapsByName.keySet();
  }

  public Map<Path, PGMMap> getMapsByPath() {
    return mapsByPath;
  }

  public @Nullable PGMMap getMapById(String mapId) {
    return mapsById.get(mapId);
  }

  public PGMMap needMapById(String mapId) {
    final PGMMap map = getMapById(mapId);
    if (map == null) {
      throw new IllegalStateException("No map with ID '" + mapId + "'");
    }
    return map;
  }

  public Optional<PGMMap> getMapByNameOrId(String nameOrId) {
    Set<PGMMap> maps = mapsByName.get(nameOrId);

    if (maps.isEmpty()) {
      return Optional.ofNullable(mapsById.get(nameOrId));
    }

    PGMMap best = null;
    for (PGMMap map : maps) {
      if (best == null || map.getSource().hasPriorityOver(best.getSource())) {
        best = map;
      }
    }

    return Optional.of(best);
  }
}
