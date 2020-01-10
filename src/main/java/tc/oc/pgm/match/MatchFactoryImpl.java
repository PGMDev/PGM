package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Difficulty;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.util.FileUtils;
import tc.oc.util.logging.ClassLogger;

public class MatchFactoryImpl implements MatchFactory {

  private final Logger logger;
  private final Server server;
  private final AtomicLong count;

  public MatchFactoryImpl(Logger logger, Server server) {
    this.logger = ClassLogger.get(logger, getClass());
    this.server = server;
    this.count = new AtomicLong(0);
  }

  @Override
  public CompletableFuture<Match> createPreMatch(MapContext map) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createPreMatchSync(map);
          } catch (Throwable t) {
            logger.log(Level.SEVERE, "Could not create pre-match for " + map.getInfo(), t);
            return null;
          }
        });
  }

  private Match createPreMatchSync(MapContext map) throws Throwable {
    checkNotNull(map);

    final String id = Long.toString(count.getAndIncrement());

    if (!map.isLoaded()) {
      map.load(); // Error will be thrown if loading fails
    }

    final File dir = new File(server.getWorldContainer(), id);
    if (dir.exists()) {
      FileUtils.delete(dir);
    }

    if (!dir.mkdirs()) {
      throw new IOException("Failed to create temporary world folder " + dir);
    }

    final MapSource source = map.getSource();
    if (source == null) {
      throw new MapNotFoundException(
          "Map source was unloaded before it could be downloaded: " + map.getInfo());
    }

    source.downloadTo(dir); // Error will be throw if map download fails

    final Match match = new MatchImpl(id, map, buildWorld("match-" + id, map));
    logger.log(Level.INFO, "Pre-loaded " + match);

    // Allow the match 1 minute to load, otherwise unload and destroy it
    server
        .getScheduler()
        .runTaskLaterAsynchronously(
            PGM.get(),
            () -> {
              if (!match.isLoaded()) {
                match.unload();
                match.destroy();

                logger.log(Level.INFO, "Discarded " + match);
              }
            },
            20 * 60);

    return match;
  }

  private World buildWorld(String worldName, MapContext map) throws Throwable {
    WorldCreator creator = server.detectWorld(worldName);
    if (creator == null) creator = new WorldCreator(worldName);

    final TerrainModule terrain = map.getModule(TerrainModule.class);
    creator
        .environment(World.Environment.NORMAL)
        .generator(terrain == null ? new NullChunkGenerator() : terrain.getChunkGenerator())
        .seed(terrain == null ? 0 : terrain.getSeed());

    final World world = createWorld(creator);
    world.setPVP(true);
    world.setSpawnFlags(false, false);
    world.setAutoSave(false);

    final Difficulty difficulty = map.getInfo().getDifficulty();
    if (difficulty != null) {
      world.setDifficulty(difficulty);
    } else if (!server.getWorlds().isEmpty()) {
      world.setDifficulty(server.getWorlds().get(0).getDifficulty());
    }

    return world;
  }

  private World createWorld(WorldCreator creator) throws Throwable {
    if (server.isPrimaryThread()) {
      return server.createWorld(creator);
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<World> world = new AtomicReference<>();
    final AtomicReference<Throwable> err = new AtomicReference<>();

    server
        .getScheduler()
        .runTask(
            PGM.get(),
            () -> {
              try {
                world.set(server.createWorld(creator));
              } catch (Throwable t) {
                err.set(t);
              } finally {
                latch.countDown();
              }
            });
    latch.await();

    final Throwable error = err.get();
    if (error != null) {
      throw error;
    }

    return world.get();
  }

  @Override
  public CompletableFuture<Boolean> createMatch(
      Match match, @Nullable Iterable<MatchPlayer> players) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createMatchSync(match, players);
          } catch (Throwable t) {
            logger.log(Level.SEVERE, "Could not create match for " + match, t);
            return false;
          }
        });
  }

  private boolean createMatchSync(Match match, @Nullable Iterable<MatchPlayer> players)
      throws Throwable {
    if (!match.isLoaded()) {
      match.load();
    }

    if (players != null) {
      players.forEach(
          player -> {
            final Match other = player.getMatch();
            final Player bukkit = player.getBukkit();

            other.removePlayer(bukkit);
            match.addPlayer(bukkit);

            if (other.isLoaded() && other.getPlayers().isEmpty()) {
              other.unload();
              other.destroy();

              logger.log(Level.INFO, "Unloaded " + other);
            }
          });
    }

    logger.log(Level.INFO, "Loaded " + match);
    return true;
  }
}
