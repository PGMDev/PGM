package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.util.FileUtils;

public class MatchFactoryImpl implements MatchFactory, Callable<Match> {
  private static final AtomicLong counter = new AtomicLong();

  private final AtomicLong start;
  private final AtomicLong timeout;
  private final Stack<Stage> stages;
  private final Future<Match> match;

  protected MatchFactoryImpl(String mapId) {
    this.start = new AtomicLong(System.currentTimeMillis());
    this.timeout = new AtomicLong(Long.MAX_VALUE);
    this.stages = new Stack<>();
    this.stages.push(new InitMapStage(checkNotNull(mapId)));
    this.match = Executors.newWorkStealingPool().submit(this);
  }

  public Match call() throws Exception {
    try {
      return run();
    } catch (Exception e) {
      PGM.get().getGameLogger().log(Level.SEVERE, "Unable to create match", e);
      throw e;
    }
  }

  private Match run() {
    Stage stage;
    Future<? extends Stage> next;

    while (!stages.empty()) {
      stage = stages.peek();
      next = stage.advance();

      // Only wait if the next stage is not done, or
      // the entire factory is not timed out.
      while (!next.isDone() && !isTimedOut()) {
        try {
          Bukkit.broadcastMessage(
              "Sleep: " + stage.getClass().getSimpleName() + " " + stage.delay());
          Thread.sleep(stage.delay());
        } catch (InterruptedException e) {
          return rollback(e);
        }
      }

      // Get the next stage and capture its exception
      final Stage done;
      try {
        done = next.get();
      } catch (ExecutionException | InterruptedException e) {
        return rollback(e);
      }

      // If there is no other stage, commit the match.
      if (done == null) {
        Bukkit.broadcastMessage("Done!");
        stages.clear();
        return stage.commit();
      } else {
        Bukkit.broadcastMessage("Ok: " + done.getClass().getSimpleName());
        stages.push(done);
      }
    }

    return rollback(
        new IllegalArgumentException("Match could not be created without an initial stage"));
  }

  private Match rollback(Exception err) {
    while (!stages.empty()) {
      stages.pop().rollback();
    }

    throw new RuntimeException(err);
  }

  private boolean isTimedOut() {
    return start.get() + timeout.get() < System.currentTimeMillis();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (isDone()) return false;
    return match.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return match.isCancelled();
  }

  @Override
  public boolean isDone() {
    return match.isDone();
  }

  @Override
  public Match get() throws InterruptedException, ExecutionException {
    return get(0, TimeUnit.MILLISECONDS);
  }

  @Override
  public Match get(long duration, TimeUnit unit) throws InterruptedException, ExecutionException {
    final long timeoutNew = Math.max(0, unit.toMillis(duration));
    timeout.getAndUpdate((timeout) -> Math.min(timeout, timeoutNew));
    return match.get();
  }

  @Override
  public void await() {
    timeout.set(0); // Will disable all delays from any stage
  }

  /** An execution stage for creating a {@link Match}. */
  private interface Stage {

    /**
     * Advance to the next stage of {@link Match} creation.
     *
     * @return A future with the next {@link Stage} or {@code null} to finish.
     */
    Future<? extends Stage> advance();

    /** Rollback any changes after attempting to {@link #advance()}. */
    default void rollback() {
      // No-op by default
    }

    /**
     * Get the number of millis to wait before the next {@link Stage}.
     *
     * @return Number of millis to wait.
     */
    default long delay() {
      return 1000L;
    }

    /**
     * Returns the completed {@link Match}, if available.
     *
     * @return A {@link Match} or {@code null} if not available.
     */
    default Match commit() {
      return null;
    }
  }

  /** Stage #1: ensures that a {@link MapContext} is loaded. */
  private static class InitMapStage implements Stage {
    private final String mapId;

    private InitMapStage(String mapId) {
      this.mapId = checkNotNull(mapId);
    }

    @Override
    public Future<DownloadMapStage> advance() {
      return PGM.get().getMapLibrary().loadExistingMap(mapId).thenApply(DownloadMapStage::new);
    }
  }

  /** Stage #2: downloads a {@link MapContext} to a local directory. */
  private static class DownloadMapStage implements Stage {
    private final MapContext map;

    private DownloadMapStage(MapContext map) {
      this.map = checkNotNull(map);
    }

    private File getDirectory() {
      return new File(PGM.get().getServer().getWorldContainer().getAbsoluteFile(), map.getId());
    }

    private InitWorldStage advanceSync() throws MapMissingException {
      rollback(); // Always ensure the directory is empty first

      final File dir = getDirectory();
      if (dir.mkdirs()) {
        map.getSource().downloadTo(dir);
      } else {
        throw new MapMissingException(dir.getPath(), "Unable to mkdirs world directory");
      }

      return new InitWorldStage(map, dir.getName());
    }

    @Override
    public Future<InitWorldStage> advance() {
      return runAsyncThread(this::advanceSync);
    }

    @Override
    public void rollback() {
      FileUtils.delete(getDirectory());
    }
  }

  /** Stage #3: initializes the {@link World} on the main thread. */
  private static class InitWorldStage implements Stage {
    private final MapContext map;
    private final String worldName;

    private InitWorldStage(MapContext map, String worldName) {
      this.map = checkNotNull(map);
      this.worldName = checkNotNull(worldName);
    }

    private InitMatchStage advanceSync() throws IllegalStateException {
      final World world =
          PGM.get()
              .getServer()
              .createWorld(
                  new WorldCreator(worldName)
                      .environment(World.Environment.NORMAL)
                      .generator(new NullChunkGenerator())
                      .seed(0L));
      if (world == null) throw new IllegalStateException("Unable to load a null world");

      world.setMetadata("map", new FixedMetadataValue(PGM.get(), map.getId()));
      world.setPVP(true);
      world.setSpawnFlags(false, false);
      world.setAutoSave(false);

      final Difficulty[] diff = Difficulty.values();
      if (map.getDifficulty() < diff.length) {
        world.setDifficulty(diff[Math.max(0, map.getDifficulty())]);
      }

      return new InitMatchStage(world, map);
    }

    @Override
    public Future<InitMatchStage> advance() {
      return runMainThread(this::advanceSync);
    }

    private boolean rollbackSync() {
      return PGM.get().getServer().unloadWorld(worldName, false);
    }

    @Override
    public void rollback() {
      runMainThread(this::rollbackSync);
    }

    @Override
    public long delay() {
      return 5 * 1000L; // 5 seconds
    }
  }

  /** Stage #4: initializes and loads the {@link Match}. */
  private static class InitMatchStage implements Stage {
    private final World world;
    private final MapContext map;

    private InitMatchStage(World world, MapContext map) {
      this.world = checkNotNull(world);
      this.map = checkNotNull(map);
    }

    private MoveMatchStage advanceSync() {
      final String id = Long.toString(counter.getAndIncrement());
      final Match match = new MatchImpl(id, map, world);

      match.load();

      return new MoveMatchStage(match);
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }

    @Override
    public void rollback() {
      runMainThread(counter::decrementAndGet);
    }
  }

  /** Stage #5: teleport {@link Player}s to the {@link Match}, with time delays. */
  private static class MoveMatchStage implements Stage {
    private static final long PLAYERS_PER_SECOND = 4;
    private final Match match;
    private final Map<Player, Match> oldMatches;

    private MoveMatchStage(Match match) {
      this.match = checkNotNull(match);
      this.oldMatches = new WeakHashMap<>();

      for (Match oldMatch : PGM.get().getMatchManager().getMatches()) {
        for (MatchPlayer player : oldMatch.getPlayers()) {
          oldMatches.put(player.getBukkit(), oldMatch);
        }
      }
    }

    private Stage advancePlayerSync(Player player, Match oldMatch) {
      if (match.getPlayer(player) != null) return null;

      Bukkit.broadcastMessage("Teleport: " + player.getName());
      oldMatch.removePlayer(player);
      match.addPlayer(player);

      return this;
    }

    private Stage advanceSync() {
      Stage stage = null;

      for (Map.Entry<Player, Match> entry : oldMatches.entrySet()) {
        stage = advancePlayerSync(entry.getKey(), entry.getValue());
        if (stage != null) break;
      }

      return stage;
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }

    private boolean rollbackSync() {
      if (match.isLoaded()) match.unload();

      for (Map.Entry<Player, Match> entry : oldMatches.entrySet()) {
        entry.getValue().addPlayer(entry.getKey());
      }
      oldMatches.clear();

      return true;
    }

    @Override
    public void rollback() {
      runMainThread(this::rollbackSync);
    }

    @Override
    public long delay() {
      return 1000L / PLAYERS_PER_SECOND;
    }

    @Override
    public Match commit() {
      return match;
    }
  }

  private static <V> Future<V> runMainThread(Callable<V> task) {
    return PGM.get().getServer().getScheduler().callSyncMethod(PGM.get(), task);
  }

  private static <V> CompletableFuture<V> runAsyncThread(Callable<V> task) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return task.call();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }
}
