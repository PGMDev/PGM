package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.server.ConfigUtils;
import tc.oc.util.logging.ClassLogger;
import tc.oc.xml.SAXHandler;

public class MapLoader {

  protected final PGM pgm;
  protected final ClassLogger logger;
  protected final ModuleRegistry factory;
  protected final SAXBuilder xmlBuilder;
  protected final ConfigurationSection sourceConfig;

  public MapLoader(PGM pgm, Logger logger, ModuleRegistry factory) {
    this.pgm = pgm;
    this.logger = ClassLogger.get(logger, getClass());
    this.factory = factory;
    this.xmlBuilder = new SAXBuilder();
    this.xmlBuilder.setSAXHandlerFactory(SAXHandler.FACTORY);
    this.sourceConfig = ConfigUtils.getOrCreateSection(pgm.getConfig(), "map");
  }

  public List<PGMMap> loadNewMaps(
      Map<Path, PGMMap> loaded, Set<Path> added, Set<Path> updated, Set<Path> removed) {
    return loadNewMaps(loadSources(), loaded, added, updated, removed);
  }

  protected List<PGMMap> loadNewMaps(
      Iterable<MapSource> sources,
      Map<Path, PGMMap> loaded,
      Set<Path> added,
      Set<Path> updated,
      Set<Path> removed) {
    checkArgument(added.isEmpty());
    checkArgument(removed.isEmpty());

    logger.fine("Loading maps...");

    Set<Path> found = new HashSet<>();
    List<PGMMap> maps = new ArrayList<>();

    for (MapSource source : sources) {
      try {
        for (Path path : source.getMapFolders(logger)) {
          try {
            found.add(path);
            PGMMap map = loaded.get(path);
            if (map == null) {
              logger.fine("  ADDED " + path);
              added.add(path);

              map = new PGMMap(pgm, factory, xmlBuilder, new MapFolder(source, path));
              if (map.reload(false)) {
                maps.add(map);
              }
            } else if (map.shouldReload()) {
              logger.fine("  UPDATED " + path);
              updated.add(path);
              map.reload(false);
            }
          } catch (MapNotFoundException e) {
            // ignore - will be removed below
          }
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Exception loading from map source " + source.getPath(), e);
      }
    }

    for (Path path : loaded.keySet()) {
      if (!found.contains(path)) {
        logger.fine("  REMOVED " + path);
        removed.add(path);
      }
    }

    logger.fine(
        "Found "
            + found.size()
            + " maps, "
            + added.size()
            + " new, "
            + removed.size()
            + " removed");

    UsernameResolver.resolveAll();

    return maps;
  }

  protected @Nullable MapSource loadDefaultMaps() {
    logger.fine("Loading default maps...");
    try {
      final byte[] buffer = new byte[1024];
      final ZipInputStream zip = new ZipInputStream(pgm.getResource("maps.zip"));
      ZipEntry entry = zip.getNextEntry();
      while (entry != null) {
        final File file =
            new File(
                pgm.getDataFolder().getAbsoluteFile(),
                new File("maps", entry.getName()).toString());
        final File parent = file.getParentFile();
        if (!entry.isDirectory() && (parent.exists() || parent.mkdirs()) && !file.exists()) {
          FileOutputStream output = new FileOutputStream(file);
          int len;
          while ((len = zip.read(buffer)) > 0) {
            output.write(buffer, 0, len);
          }
          output.close();
        }
        entry = zip.getNextEntry();
      }
      zip.closeEntry();
      zip.close();
      return new MapSource(
          new File(pgm.getDataFolder().getAbsoluteFile(), "maps").toPath(),
          null,
          Integer.MAX_VALUE,
          new HashSet<>(),
          new HashSet<>(),
          Integer.MAX_VALUE);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Exception loading default maps", e);
    }
    return null;
  }

  protected List<MapSource> loadSources() {
    logger.fine("Loading map sources...");
    List<MapSource> sources = new ArrayList<>();
    ConfigurationSection config = ConfigUtils.getOrCreateSection(sourceConfig, "sources");

    if (sourceConfig.getBoolean("default", false)) {
      MapSource source = loadDefaultMaps();
      if (source != null) {
        sources.add(source);
      }
    }

    for (String key : config.getKeys(false)) {
      ConfigurationSection section = config.getConfigurationSection(key);
      try {
        sources.add(loadSource(key, section));
      } catch (InvalidConfigurationException e) {
        this.logger.warning("Failed to parse maps source: " + e.getMessage());
      }
    }

    Collections.sort(sources);
    logger.fine("Loaded " + sources.size() + " sources");
    return sources;
  }

  protected MapSource loadSource(String key, ConfigurationSection section)
      throws InvalidConfigurationException {
    URL sourceUrl = ConfigUtils.getUrl(section, "url", null);
    Path sourcePath = ConfigUtils.getPath(section, "path", null);

    if (sourcePath != null && !sourcePath.isAbsolute()) {
      Path serverRoot =
          pgm.getDataFolder().getAbsoluteFile().getParentFile().getParentFile().toPath();
      sourcePath = serverRoot.resolve(sourcePath);
    }
    if (sourcePath == null || !Files.isDirectory(sourcePath)) {
      throw new InvalidConfigurationException(
          "Skipping '" + key + "' because it does not have a valid path");
    }

    int depth = section.getInt("depth", Integer.MAX_VALUE);
    int priority = section.getInt("priority", 0);

    Set<Path> excludedPaths = loadPathList(sourcePath, section.getStringList("exclude"));
    Set<Path> onlyPaths = loadPathList(sourcePath, section.getStringList("only"));

    MapSource source =
        new MapSource(sourcePath, sourceUrl, depth, onlyPaths, excludedPaths, priority);
    logger.fine("  " + source);
    return source;
  }

  private Set<Path> loadPathList(Path base, Collection<String> values) {
    Set<Path> paths = new HashSet<>();
    for (String value : values) {
      paths.add(Paths.get(value));
    }
    return paths;
  }
}
