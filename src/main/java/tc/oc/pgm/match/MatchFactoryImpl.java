package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.time.Duration;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.WorldInfo;
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
    this.timeout = new AtomicLong(Config.Experiments.get().getMatchPreLoadSeconds() * 1000L);
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
          Thread.sleep(stage.delay().toMillis());
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
        stages.clear();
        return stage.commit();
      } else {
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
     * Get the duration to wait before the next {@link Stage}.
     *
     * @return Duration to wait.
     */
    default Duration delay() {
      return Duration.ofSeconds(1);
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
    private File dir;

    private DownloadMapStage(MapContext map) {
      this.map = checkNotNull(map);
    }

    private File getDirectory() {
      if (dir == null) {
        dir =
            new File(
                PGM.get().getServer().getWorldContainer().getAbsoluteFile(),
                "match-" + counter.getAndIncrement());
      }
      return dir;
    }

    private InitWorldStage advanceSync() throws MapMissingException {
      FileUtils.delete(getDirectory()); // Always ensure the directory is empty first

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
      counter.getAndDecrement();
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

    private Stage advanceSync() throws IllegalStateException {
      final WorldInfo info = map.getWorld();
      final World world =
          PGM.get()
              .getServer()
              .createWorld(
                  new WorldCreator(worldName)
                      .environment(World.Environment.values()[info.getEnvironment()])
                      .generator(info.hasTerrain() ? null : new NullChunkGenerator())
                      .seed(info.getSeed()));
      if (world == null) throw new IllegalStateException("Unable to load a null world");

      world.setPVP(true);
      world.setSpawnFlags(false, false);
      world.setAutoSave(false);
      world.setDifficulty(Difficulty.values()[map.getDifficulty()]);

      return new InitMatchStage(world, map);
    }

    @Override
    public Future<Stage> advance() {
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
    public Duration delay() {
      return Duration.ofSeconds(5);
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
      final String id = Long.toString(counter.get() - 1);
      final Match match = new MatchImpl(id, map, world);

      match.load();

      return new MoveMatchStage(match);
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }
  }

  /** Stage #5: teleport {@link Player}s to the {@link Match}, with time delays. */
  private static class MoveMatchStage implements Stage {
    private final Match match;

    private MoveMatchStage(Match match) {
      this.match = checkNotNull(match);
    }

    private Stage advanceSync() {
      for (Match otherMatch : PGM.get().getMatchManager().getMatches()) {
        if (match.equals(otherMatch)) continue;
        for (MatchPlayer player : otherMatch.getPlayers()) {
          final Player bukkit = player.getBukkit();

          otherMatch.removePlayer(bukkit);
          match.addPlayer(bukkit);

          return this;
        }
        otherMatch.unload();
      }

      return null;
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }

    @Override
    public Duration delay() {
      return Duration.ofMillis(
          Duration.ofSeconds(1).toMillis()
              / Config.Experiments.get().getPlayerTeleportsPerSecond());
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
