package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.time.Duration;
import java.util.Iterator;
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
  private final Future<Match> future;

  protected MatchFactoryImpl(String mapId) {
    this.start = new AtomicLong(System.currentTimeMillis());
    this.timeout = new AtomicLong(Config.Experiments.get().getMatchPreLoadSeconds() * 1000L);
    this.stages = new Stack<>();
    this.stages.push(new InitMapStage(checkNotNull(mapId)));
    this.future = Executors.newSingleThreadExecutor().submit(this);
  }

  public Match call() {
    try {
      return run();
    } catch (Exception e) {
      // Match creation was cancelled, no need to show an error
      if (e.getCause() instanceof InterruptedException) throw e;

      final Throwable err = e.getCause();
      PGM.get().getGameLogger().log(Level.SEVERE, err.getMessage(), err.getCause());
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
      final long delay = stage.delay().toMillis();
      while (!next.isDone() && start.get() + timeout.get() + delay >= System.currentTimeMillis()) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          return revert(e);
        }
      }

      // Get the next stage and capture its exception
      final Stage done;
      try {
        done = next.get();
      } catch (ExecutionException | InterruptedException e) {
        return revert(e);
      }

      // If there is no other stage, commit the match.
      if (done == null) {
        stages.clear();
        if (stage instanceof Commitable) {
          return ((Commitable) stage).commit();
        }
        return revert(new IllegalStateException("Unable to load match with an incomplete stage"));
      } else {
        stages.push(done);
      }
    }

    return revert(new IllegalStateException("Unable to load match without an initial stage"));
  }

  private Match revert(Exception err) {
    while (!stages.empty()) {
      final Stage stage = stages.pop();
      if (stage instanceof Revertable) {
        ((Revertable) stage).revert();
      } else {
        throw new IllegalStateException("Unable to revert a loaded match");
      }
    }

    throw new RuntimeException(err);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (isDone()) return false;
    return future.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public Match get() throws InterruptedException, ExecutionException {
    return get(0, TimeUnit.MILLISECONDS);
  }

  @Override
  public Match get(long duration, TimeUnit unit) throws InterruptedException, ExecutionException {
    final long timeoutNew = Math.max(0, unit.toMillis(duration));
    timeout.getAndUpdate((timeout) -> Math.min(timeout, timeoutNew));
    return future.get();
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

    /**
     * Get the duration to wait before the next {@link Stage}.
     *
     * @return Duration to wait.
     */
    default Duration delay() {
      return Duration.ZERO;
    }
  }

  /** An execution {@link Stage} that can revert its changes. */
  private interface Revertable {

    /** Revert any changes after attempting to {@link Stage#advance()}. */
    void revert();
  }

  /** An execution {@link Stage} that can return a {@link Match}. */
  private interface Commitable {

    /**
     * Returns the completed {@link Match}.
     *
     * @return A {@link Match}.
     */
    Match commit();
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
  private static class DownloadMapStage implements Stage, Revertable {
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
    public void revert() {
      counter.getAndDecrement();
      FileUtils.delete(getDirectory());
    }
  }

  /** Stage #3: initializes the {@link World} on the main thread. */
  private static class InitWorldStage implements Stage, Revertable {
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

    private boolean revertSync() {
      return PGM.get().getServer().unloadWorld(worldName, false);
    }

    @Override
    public void revert() {
      runMainThread(this::revertSync);
    }

    @Override
    public Duration delay() {
      return Duration.ofSeconds(3);
    }
  }

  /** Stage #4: initializes and loads the {@link Match}. */
  private static class InitMatchStage implements Stage, Revertable, Commitable {
    private final Match match;

    private InitMatchStage(World world, MapContext map) {
      this.match =
          new MatchImpl(Long.toString(counter.get() - 1), checkNotNull(map), checkNotNull(world));
    }

    private MoveMatchStage advanceSync() {
      final boolean move = PGM.get().getMatchManager().getMatches().hasNext();
      match.load();
      return move ? new MoveMatchStage(match) : null;
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }

    private boolean revertSync() {
      match.unload();
      match.destroy();
      return true;
    }

    @Override
    public void revert() {
      runMainThread(this::revertSync);
    }

    @Override
    public Match commit() {
      return match;
    }
  }

  /** Stage #5: teleport {@link Player}s to the {@link Match}, with time delays. */
  private static class MoveMatchStage implements Stage, Commitable {
    private final Match match;
    private final Duration delay;

    private MoveMatchStage(Match match) {
      this.match = checkNotNull(match);
      this.delay =
          Duration.ofMillis(1000L / Config.Experiments.get().getPlayerTeleportsPerSecond());
    }

    private Stage advanceSync() {
      final Iterator<Match> iterator = PGM.get().getMatchManager().getMatches();
      while (iterator.hasNext()) {
        final Match otherMatch = iterator.next();
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
      return delay;
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
