package tc.oc.pgm.match;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Listener;
import tc.oc.pgm.map.MapInfo;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.util.FileUtils;

/** Provides handy methods to load, save, and archive match worlds. */
public class WorldManager implements Listener {
  private final Server server;
  private final Map<World, PGMMap> mapsByWorld = Maps.newHashMap();

  public WorldManager(Server server) {
    this.server = server;
  }

  /** Copies the new map's world directory to the specified destination. */
  public void copyWorld(File source, File destination) throws IOException {
    // only copy level.dat, region/, and data/
    if (!destination.mkdir()) {
      throw new IOException("Failed to create temporary world folder " + destination);
    }

    FileUtils.copy(new File(source, "level.dat"), new File(destination, "level.dat"));

    File region = new File(source, "region");
    if (region.isDirectory()) {
      FileUtils.copy(region, new File(destination, "region"));
    }

    File data = new File(source, "data");
    if (data.isDirectory()) {
      FileUtils.copy(data, new File(destination, "data"));
    }
  }

  /** Registers a given world with Bukkit. */
  public World createWorld(String worldName, PGMMap map) throws IllegalArgumentException {
    MapInfo info = map.getInfo();
    TerrainModule terrain = map.getContext().needModule(TerrainModule.class);
    WorldCreator creator = server.detectWorld(worldName);
    if (creator == null) creator = new WorldCreator(worldName);
    creator
        .environment(info.dimension)
        .generator(terrain.getChunkGenerator())
        .seed(terrain.getSeed());

    World world = this.server.createWorld(creator);
    if (world == null) {
      throw new IllegalArgumentException("Failed to create world");
    }

    this.mapsByWorld.put(world, map);

    world.setSpawnFlags(false, false); // disable spawning
    world.setAutoSave(false); // disable auto saving

    if (info.difficulty != null) {
      world.setDifficulty(info.difficulty);
    } else {
      world.setDifficulty(this.server.getWorlds().get(0).getDifficulty()); // default difficulty
    }

    // Unload any worlds that are not matches, such as the default world
    for (World w : this.server.getWorlds()) {
      if (!w.getName().startsWith("match")) {
        unloadWorld(w);
      }
    }

    return world;
  }

  public void unloadWorld(World world) {
    this.mapsByWorld.remove(world);
    this.server.unloadWorld(world, false);
  }

  public PGMMap getMap(World world) {
    return this.mapsByWorld.get(world);
  }

  /** Copies the world to an archive or deletes it if no archive exists. */
  public void archive(String worldName, @Nullable File outputDirectory) {
    File file = new File(this.server.getWorldContainer(), worldName);
    if (!file.exists()) return;

    if (outputDirectory != null) {
      this.cleanWorldDirectory(file);
      file.renameTo(outputDirectory);
    } else {
      FileUtils.delete(file);
    }
  }

  /**
   * Cleans the world directory of generated files. Currently it deletes: - session.lock - uid.dat
   *
   * @param dir File pointing to the world directory.
   */
  public void cleanWorldDirectory(File dir) {
    new File(dir, "session.lock").delete();
    new File(dir, "uid.dat").delete();
  }
}
