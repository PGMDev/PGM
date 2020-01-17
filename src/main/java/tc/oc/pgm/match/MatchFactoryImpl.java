package tc.oc.pgm.match;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.util.FileUtils;
import tc.oc.util.ServerThreadLock;
import tc.oc.util.logging.ClassLogger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

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
    // FIXME: async world creation leads to deadlock
    return CompletableFuture.completedFuture(createPreMatchSync(map));
  }

  private Match createPreMatchSync(MapContext map) throws MapNotFoundException {
    checkNotNull(map);
    checkNotNull(map.getSource());

    final String id = Long.toString(count.getAndIncrement());
    final String name = "match-" + id;
    final File dir = new File(server.getWorldContainer().getAbsoluteFile(), name);
    try {
        if (dir.exists()) {
            FileUtils.delete(dir);
        }

        if (!dir.mkdirs()) {
            throw new IOException("Could not create directories for " + dir.getPath());
        }

        map.getSource().downloadTo(dir);
    } catch (IOException e) {
        throw new MapNotFoundException(map, "Could not download map files", e);
    }

    final Match match = new MatchImpl(id, map, createWorld(name, map));
    logger.log(Level.INFO, "Pre-loaded: #" + match.getId() + " " + match.getMap().getId());
    return match;
  }

  private World createWorld(String worldName, MapContext map) {
    WorldCreator creator = server.detectWorld(worldName);
    if (creator == null) creator = new WorldCreator(worldName);

    final TerrainModule terrain = map.getModule(TerrainModule.class);
    creator
        .environment(World.Environment.NORMAL)
        .generator(terrain == null ? new NullChunkGenerator() : terrain.getChunkGenerator())
        .seed(terrain == null ? 0 : terrain.getSeed());

    final World world;
    try (final ServerThreadLock lock = ServerThreadLock.acquire()) {
        world = server.createWorld(creator);
    }
    world.setPVP(true);
    world.setSpawnFlags(false, false);
    world.setAutoSave(false);
    world.setDifficulty(map.getDifficulty());

    return world;
  }

  @Override
  public void createMatch(Match match, @Nullable Iterable<MatchPlayer> players) {
    if (match == null) return;
    if (!match.isLoaded()) {
      match.load();
    }

    logger.log(Level.INFO, "Loaded: #" + match.getId() + " " + match.getMap().getId());
    if(players == null) return;

    for(MatchPlayer player : players) {
        final Match oldMatch = player.getMatch();
        final Player bukkit = player.getBukkit();

        oldMatch.removePlayer(bukkit);
        match.addPlayer(bukkit);

        try {
            if (oldMatch.getPlayers().isEmpty()) {
                oldMatch.unload();
                oldMatch.destroy();

                logger.log(Level.INFO, "Unloaded: #" + oldMatch.getId() + " " + match.getMap().getId());
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Could not unload empty match", t);
        }
    }
  }
}
