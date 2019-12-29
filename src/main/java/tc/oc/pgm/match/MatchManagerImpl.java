package tc.oc.pgm.match;

import com.google.common.cache.*;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.entity.Player;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.chat.MultiAudience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.map.*;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.rotation.PGMMapOrder;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.util.FileUtils;
import tc.oc.util.logging.ClassLogger;
import tc.oc.world.NMSHacks;

@SuppressWarnings("UnstableApiUsage")
public class MatchManagerImpl implements MatchManager, MultiAudience {

  private final Logger logger;
  private final Server server;
  private final MapLibrary library;
  private final MapLoader loader;

  private final Map<String, Match> matchById = new HashMap<>();
  private final Map<String, String> matchIdByWorldName = new HashMap<>();
  private final LoadingCache<PGMMap, String> preMatch =
      CacheBuilder.newBuilder()
          .expireAfterWrite(10, TimeUnit.SECONDS)
          // If after 10 secs of writing the match it hasn't been loaded, unload & destroy it
          .removalListener(
              (RemovalNotification<PGMMap, String> r) -> {
                if (!matchById.get(r.getValue()).isLoaded()) unloadMatch(r.getValue());
              })
          .concurrencyLevel(1)
          .build(
              new CacheLoader<PGMMap, String>() {
                @Override
                public String load(@Nonnull PGMMap pgmMap) throws Exception {
                  return createPreMatch(pgmMap).getId();
                }
              });
  private final AtomicInteger count;

  private PGMMapOrder pgmMapOrder;

  public MatchManagerImpl(Server server, MapLibrary library, MapLoader loader)
      throws MapNotFoundException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), getClass());
    this.server = server;
    this.library = library;
    this.loader = loader;
    this.count = new AtomicInteger(0);

    loadNewMaps();
  }

  public void createPreMatchAsync(final PGMMap map) {
    logger.fine("Creating pre-match for " + map.getName() + " async");
    PGM.get()
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            PGM.get(),
            () -> {
              try {
                preMatch.get(map);
                PGM.get()
                    .getLogger()
                    .fine("Done creating pre-match for " + map.getName() + " async");
              } catch (Throwable t) {
                throw new RuntimeException(t);
              }
            });
  }

  /**
   * Pre-creates a match, without loading it. Can and should be done async unless a /cycle 0 is
   * done.
   *
   * @param map The map to generate a pre-match for
   * @return The created pre-match
   * @throws Exception If the match fails to be generated
   */
  private Match createPreMatch(PGMMap map) throws Exception {
    logger.fine("Creating pre-match for " + map.getName());
    String id = Integer.toString(count.getAndIncrement());

    if (!map.getContext().isPresent()) {
      map.reload(true);
    }

    final TerrainModule terrain = map.getContext().get().needModule(TerrainModule.class);
    final File src = terrain.getWorldFolder();

    final String worldName = createMatchFolder(id, src);
    final World world = createMatchWorld(worldName, map.getInfo(), terrain);

    final Match match = new MatchImpl(id, map, world);

    matchById.put(match.getId(), match);
    matchIdByWorldName.put(match.getWorld().getName(), match.getId());

    logger.fine("Done creating pre-match for " + map.getName());
    return match;
  }

  @Override
  public Match createMatch(PGMMap map) throws Throwable {
    final Match match = matchById.get(preMatch.get(map));

    try {
      match.load();
    } catch (Throwable t) {
      unloadMatch(match.getId());
      throw t;
    } finally {
      // Remove all preMatches, since we already loaded one
      preMatch.invalidateAll();
    }

    return match;
  }

  private String createMatchFolder(String id, File src) throws IOException {
    final String worldName = getWorldName(id);
    final File dest = new File(server.getWorldContainer(), worldName);

    if (dest.exists()) FileUtils.delete(dest);

    if (!dest.mkdir()) {
      throw new IOException("Failed to create temporary world folder " + dest);
    }

    FileUtils.copy(new File(src, "level.dat"), new File(dest, "level.dat"));

    File region = new File(src, "region");
    if (region.isDirectory()) {
      FileUtils.copy(region, new File(dest, "region"));
    }

    File data = new File(src, "data");
    if (data.isDirectory()) {
      FileUtils.copy(data, new File(dest, "data"));
    }

    return worldName;
  }

  private World createMatchWorld(String worldName, MapInfo info, TerrainModule terrain) {
    WorldCreator creator = server.detectWorld(worldName);
    if (creator == null) creator = new WorldCreator(worldName);
    creator
        .environment(info.dimension)
        .generator(terrain.getChunkGenerator())
        .seed(terrain.getSeed());

    final World world = createWorld(creator);
    if (world == null) {
      throw new IllegalStateException("Failed to create world, createWorld returned null");
    }

    world.setPVP(true);
    world.setSpawnFlags(false, false);
    world.setAutoSave(false);

    if (info.difficulty != null) {
      world.setDifficulty(info.difficulty);
    } else {
      world.setDifficulty(server.getWorlds().get(0).getDifficulty());
    }

    return world;
  }

  /**
   * Creates a new world, wich must be done on the primary thread. If already running on the primary
   * thread, calls createWorld, otherwise, starts a sync task to run create world, and waits until
   * it's done.
   *
   * @param creator The creator for the world
   * @return A world if it was able to be created, null otherwise
   */
  private World createWorld(final WorldCreator creator) {
    if (server.isPrimaryThread()) return server.createWorld(creator);

    // FIXME: This is pretty dirty, there must be a better way to run & wait on main
    final Object LOCK = new Object();
    final AtomicReference<World> world = new AtomicReference<>();
    final AtomicBoolean isDone = new AtomicBoolean();
    PGM.get()
        .getServer()
        .getScheduler()
        .runTask(
            PGM.get(),
            () -> {
              world.set(server.createWorld(creator));
              isDone.set(true);
              synchronized (LOCK) {
                LOCK.notifyAll();
              }
            });
    while (!isDone.get()) {
      synchronized (LOCK) {
        try {
          LOCK.wait();
        } catch (InterruptedException ignore) {
        }
      }
    }
    return world.get();
  }

  @Override
  public Collection<Match> getMatches() {
    return ImmutableSet.copyOf(matchById.values());
  }

  @Override
  public void unloadMatch(@Nullable String id) {
    final Match match = matchById.get(id);
    if (match == null) return;

    if (match.isRunning()) {
      match.finish();
    }

    if (match.isLoaded()) {
      match.unload();
    }

    matchIdByWorldName.remove(getWorldName(id));
    matchById.remove(id);
    int destroyDelay = Config.Experiments.get().getDestroyMatchDelay();
    if (destroyDelay <= 0) match.destroy();
    else {
      // Destroy the match after unload, to let the server catch-up
      PGM.get()
          .getServer()
          .getScheduler()
          .runTaskLaterAsynchronously(PGM.get(), match::destroy, destroyDelay);
    }
  }

  @Override
  public Optional<Match> cycleMatch(@Nullable Match oldMatch, PGMMap nextMap, boolean retry) {
    // Match unload also does this, but doing it earlier avoids some problems.
    // Specifically, RestartCountdown cannot cancel itself during a cycle.
    if (oldMatch != null) {
      oldMatch.getCountdown().cancelAll();
    }

    Set<PGMMap> failed = new HashSet<>(); // Don't try any map more than once

    // Try to load a rotation map
    int maxCycles = library.getMaps().size();
    for (int cycles = 0; cycles < maxCycles; cycles++) {

      if (!failed.contains(nextMap)) {
        Match match = cycleSafe(oldMatch, nextMap);
        if (match != null) return Optional.of(match);
      }

      // If retryRotation is false, give up after the first failure
      if (!retry) return Optional.empty();

      failed.add(nextMap);
    }

    // If all rotation maps failed, and we're not allowed to try non-rotation maps, give up
    if (!retry) return Optional.empty();

    // Try every map in the library
    for (PGMMap map : library.getMaps()) {
      if (!failed.contains(map)) {
        Match match = cycleSafe(oldMatch, map);
        if (match != null) return Optional.of(match);
      }
      failed.add(map);
    }

    return Optional.empty();
  }

  /**
   * Call {@link #cycleUnsafe(Match, PGMMap)} and log any exceptions to the map logger.
   *
   * @return the new match, or null if loading failed
   */
  private @Nullable Match cycleSafe(@Nullable Match oldMatch, PGMMap newMap) {
    try {
      return this.cycleUnsafe(oldMatch, newMap);
    } catch (MapNotFoundException e) {
      // Maps are sometimes removed, must handle it gracefully
      logger.warning("Skipping deleted map " + newMap.getName());
      try {
        loadNewMaps();
      } catch (MapNotFoundException not) {
        logger.severe("No maps could be loaded, server cannot cycle");
      }
      return null;
    } catch (ModuleLoadException e) {
      if (e.getModule() != null) {
        newMap
            .getLogger()
            .log(
                Level.SEVERE,
                "Exception loading module " + e.getModule().getName() + ": " + e.getMessage(),
                e);
      } else {
        newMap.getLogger().log(Level.SEVERE, "Exception loading map: " + e.getMessage(), e);
      }
    } catch (Throwable e) {
      newMap.getLogger().log(Level.SEVERE, "Exception loading map: " + e.getMessage(), e);
    }
    return null;
  }

  @Override
  public Collection<PGMMap> loadNewMaps() throws MapNotFoundException {
    Set<Path> added = new HashSet<>(), updated = new HashSet<>(), removed = new HashSet<>();
    List<PGMMap> maps = loader.loadNewMaps(library.getMapsByPath(), added, updated, removed);
    library.removeMaps(removed);
    Set<PGMMap> newMaps = library.addMaps(maps);

    logger.info("Loaded " + newMaps.size() + " maps");

    if (library.getMaps().isEmpty()) {
      throw new MapNotFoundException();
    }

    return newMaps;
  }

  @Override
  public void setMapOrder(PGMMapOrder pgmMapOrder) {
    this.pgmMapOrder = pgmMapOrder;
  }

  @Override
  public PGMMapOrder getMapOrder() {
    return pgmMapOrder;
  }

  /**
   * Creates and loads a new {@link Match} on the given map, optionally unloading an old match and
   * transferring all players to the new one.
   *
   * @param oldMatch if given, this match is unloaded and all players are transferred to the new
   *     match
   * @param newMap the map to load for the new match
   * @return the newly loaded {@link Match}
   * @throws Throwable generally, any exceptions thrown during loading/unloading are propagated
   */
  private Match cycleUnsafe(@Nullable Match oldMatch, PGMMap newMap) throws Throwable {
    logger.info("Cycling to " + newMap.toString());

    newMap.reload(true);

    if (oldMatch != null) oldMatch.finish();

    Match newMatch = this.createMatch(newMap);

    if (oldMatch != null) {
      Set<Player> players = new HashSet<>(oldMatch.getPlayers().size());
      for (MatchPlayer matchPlayer : oldMatch.getPlayers()) {
        players.add(matchPlayer.getBukkit());
      }

      // First unload, required to remove players & stop listeners
      oldMatch.unload();
      boolean doubleTp = !Config.Experiments.get().isAvoidDoubleTp();
      for (Player player : players) {
        NMSHacks.forceRespawn(player);
        // Players will be teleported when being added to the match either way.
        if (doubleTp) {
          player.teleport(newMatch.getWorld().getSpawnLocation());
        }
        player.setArrowsStuck(0);
        newMatch.addPlayer(player);
      }

      // The second unload, will get rid of the links to match, and call destroy asynchronously
      this.unloadMatch(oldMatch.getId());
    }

    logger.info("Loaded " + newMap.toString());

    return newMatch;
  }

  @Nullable
  @Override
  public Match getMatch(@Nullable World world) {
    if (world == null) return null;
    final String matchId = matchIdByWorldName.get(world.getName());
    if (matchId == null) return null;
    return matchById.get(matchId);
  }

  @Nullable
  @Override
  public MatchPlayer getPlayer(@Nullable Player player) {
    if (player == null) return null;
    final Match match = getMatch(player.getWorld());
    if (match == null) return null;
    return match.getPlayer(player);
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getMatches();
  }

  private static String getWorldName(String matchId) {
    return "match-" + matchId;
  }
}
