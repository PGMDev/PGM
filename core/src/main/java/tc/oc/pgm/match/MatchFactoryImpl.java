package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public class MatchFactoryImpl implements MatchFactory, Callable<Match> {
  private static final AtomicLong counter = new AtomicLong();
  private static final Difficulty[] difficulties = Difficulty.values();
  private static final World.Environment[] environments = World.Environment.values();

  private final Stack<Stage> stages;
  private final Future<Match> future;
  private final AtomicBoolean timedOut;

  protected MatchFactoryImpl(String mapId) {
    this.stages = new Stack<>();
    this.stages.push(new InitMapStage(checkNotNull(mapId)));
    this.timedOut = new AtomicBoolean(false);
    this.future = Executors.newSingleThreadExecutor().submit(this);
  }

  public Match call() {
    try {
      return run();
    } catch (Exception e) {
      // Match creation was cancelled, no need to show an error
      if (e.getCause() instanceof InterruptedException) throw e;

      Throwable err = e.getCause();
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
      while (!next.isDone() && !timedOut.get()) {
        try {
          Thread.sleep(Math.max(100, delay));
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
    return future.get();
  }

  @Override
  public void await() {
    timedOut.set(true); // Will disable all delays from any stage
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
      WorldCreator creator = NMSHacks.detectWorld(worldName);
      if (creator == null) {
        creator = new WorldCreator(worldName);
      }
      final World world =
          PGM.get()
              .getServer()
              .createWorld(
                  creator
                      .environment(environments[info.getEnvironment()])
                      .generator(info.hasTerrain() ? null : NullChunkGenerator.INSTANCE)
                      .seed(info.hasTerrain() ? info.getSeed() : creator.seed()));
      if (world == null) throw new IllegalStateException("Unable to load a null world");

      world.setPVP(true);
      world.setSpawnFlags(false, false);
      world.setAutoSave(false);
      world.setDifficulty(difficulties[map.getDifficulty()]);

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

      // Right before moving players, make sure they don't show up in tab list due to missing a team
      for (Player viewer : Bukkit.getOnlinePlayers()) {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (viewer.canSee(player) && viewer != player) players.add(player.getName());
        }
        NMSHacks.sendPacket(
            viewer,
            NMSHacks.teamCreatePacket(
                "dummy", "dummy", ChatColor.AQUA.toString(), "", false, false, players));
      }

      Duration delay = Duration.ZERO;
      try {
        delay =
            Duration.ofMillis(
                (long)
                    (1000f
                        / TextParser.parseFloat(
                            PGM.get()
                                .getConfiguration()
                                .getExperiments()
                                .getOrDefault("match-teleports-per-second", "")
                                .toString(),
                            Range.atLeast(1f))));
      } catch (TextException e) {
        // No-op, since an experimental feature
      }
      this.delay = delay;
    }

    private Stage advanceSync() {
      // Create copy to avoid CME on mach unload
      for (Match otherMatch : Lists.newArrayList(PGM.get().getMatchManager().getMatches())) {
        if (match.equals(otherMatch)) continue;

        for (MatchPlayer player : otherMatch.getPlayers()) {
          final Player bukkit = player.getBukkit();

          otherMatch.removePlayer(bukkit);
          match.addPlayer(bukkit);

          return this;
        }
        otherMatch.unload();
      }

      // After all players have been teleported, remove the dummy team
      NMSHacks.sendPacket(NMSHacks.teamRemovePacket("dummy"));

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
