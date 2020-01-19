package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.joda.time.Duration;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.server.Scheduler;
import tc.oc.util.FileUtils;
import tc.oc.util.logging.ClassLogger;

public class MatchManagerImpl implements MatchFactory, MatchManager {

  private static final Duration TIMEOUT = Duration.standardSeconds(30);

  private final Logger logger;
  private final Server server;
  private final Scheduler scheduler;

  private final AtomicLong matches;
  private final Map<String, Match> matchById;
  private final Map<String, Match> matchByWorld;

  public MatchManagerImpl(Logger logger, Server server) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.server = checkNotNull(server);
    this.scheduler = new Scheduler(PGM.get());
    this.matches = new AtomicLong(0);
    this.matchById = new ConcurrentHashMap<>();
    this.matchByWorld = new ConcurrentHashMap<>();
  }

  @Override
  public Match getMatch(@Nullable World world) {
    return matchByWorld.get(world == null ? null : world.getName());
  }

  @Override
  public Iterable<Match> getMatches() {
    return Iterables.filter(matchById.values(), Match::isLoaded);
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getMatches();
  }

  @Override
  public MatchPlayer getPlayer(@Nullable Player bukkit) {
    // FIXME: determine if this needs to be more efficient with N matches
    for (Match match : getMatches()) {
      final MatchPlayer player = match.getPlayer(bukkit);
      if (player != null) {
        return player;
      }
    }
    return null;
  }

  @Override
  public CompletableFuture<Match> initMatch(MapContext map) {
    return CompletableFuture.supplyAsync(() -> initMatchAsync(map));
  }

  private Match initMatchAsync(MapContext map) throws MapNotFoundException {
    checkNotNull(map);
    checkNotNull(map.getSource());

    final String id = Long.toString(matches.getAndIncrement());
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

    final World world;
    try {
      world = initWorld(name, map).get(TIMEOUT.getStandardSeconds(), TimeUnit.MINUTES);
    } catch (Throwable t) {
      throw new MapNotFoundException("Could not load world", t);
    }

    final Match match = new MatchImpl(id, map, world);
    logger.log(Level.INFO, "Pre-loaded: #" + match.getId() + " " + match.getMap().getName());
    return match;
  }

  private Future<World> initWorld(String worldName, MapContext map) {
    final TerrainModule terrain = map.getModule(TerrainModule.class);
    final WorldCreator creator =
        new WorldCreator(worldName)
            .environment(World.Environment.NORMAL)
            .generator(terrain == null ? new NullChunkGenerator() : terrain.getChunkGenerator())
            .seed(terrain == null ? new Random().nextLong() : terrain.getSeed());

    return scheduler.runMainThread(
        () -> {
          final World world = server.createWorld(creator);
          world.setPVP(true);
          world.setSpawnFlags(false, false);
          world.setAutoSave(false);
          world.setDifficulty(map.getDifficulty());
          return world;
        });
  }

  @Override
  public void moveMatch(@Nullable Match oldMatch, Match match) {
    scheduler.runMainThread(
        () -> {
          moveMatchSync(oldMatch, match);
          return true;
        });
  }

  private void destroyMatch(Match match) {
    match.destroy();
    logger.log(Level.INFO, "Destroyed: #" + match.getId() + " " + match.getMap().getName());
  }

  private void unloadMatch(Match match) {
    matchById.remove(match.getId());
    for (Map.Entry<String, Match> entry : matchByWorld.entrySet()) {
      if (entry.getValue().equals(match)) {
        matchByWorld.remove(entry.getKey());
      }
    }

    match.unload();
    logger.log(Level.INFO, "Unloaded: #" + match.getId() + " " + match.getMap().getName());

    scheduler.runTaskLater(TIMEOUT, () -> destroyMatch(match));
  }

  private void moveMatchSync(@Nullable Match oldMatch, Match match) {
    checkNotNull(match);
    checkArgument(server.isPrimaryThread());

    if (!match.isLoaded()) {
      match.load();
    }
    matchById.put(match.getId(), match);
    matchByWorld.put(match.getWorld().getName(), match);

    logger.log(Level.INFO, "Loaded: #" + match.getId() + " " + match.getMap().getName());

    if (oldMatch == null) return;

    for (MatchPlayer player : oldMatch.getPlayers()) {
      oldMatch.removePlayer(player.getBukkit());
      match.addPlayer(player.getBukkit());
    }

    unloadMatch(oldMatch);
  }
}
