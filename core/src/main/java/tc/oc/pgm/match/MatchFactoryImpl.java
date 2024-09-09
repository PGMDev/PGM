package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.Packets.TAB_PACKETS;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchFactoryImpl implements MatchFactory, Callable<Match> {
  private static final AtomicLong counter = new AtomicLong();
  private static final Difficulty[] difficulties = Difficulty.values();
  private static final World.Environment[] environments = World.Environment.values();

  private static final String DUMMY_TEAM = "dummy";

  private final Stack<Stage> stages;
  private final Future<Match> future;
  private final AtomicBoolean timedOut;

  protected MatchFactoryImpl(String mapId) {
    this.stages = new Stack<>();
    this.stages.push(new InitMapStage(assertNotNull(mapId)));
    this.timedOut = new AtomicBoolean(false);
    this.future = Executors.newSingleThreadExecutor().submit(this);
  }

  public Match call() {
    try {
      return run();
    } catch (Exception e) {
      // Match creation was cancelled, no need to show an error
      if (e.getCause() instanceof InterruptedException) throw e;

      Throwable err = Objects.requireNonNullElse(e.getCause(), e);
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
      final long delay = stage.delay(timedOut.get()).toMillis();
      while (!next.isDone() && delay > 0) {
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
     * @param timedOut If the factory has timed out, meaning it needs to move quickly.
     * @return Duration to wait.
     */
    default Duration delay(boolean timedOut) {
      return timedOut ? Duration.ZERO : Duration.ofMillis(100);
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
      this.mapId = assertNotNull(mapId);
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
      this.map = assertNotNull(map);
    }

    private File getDirectory() {
      if (dir == null) {
        dir = new File(
            PGM.get().getServer().getWorldContainer().getAbsoluteFile(),
            "match-" + counter.getAndIncrement());
      }
      return dir;
    }

    private InitWorldStage advanceSync() throws MapMissingException {
      FileUtils.delete(getDirectory()); // Always ensure the directory is empty first

      final File dir = getDirectory();
      if (dir.mkdirs()) {
        map.getInfo().getSource().downloadTo(map.getInfo().getWorldFolder(), dir);
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
      this.map = assertNotNull(map);
      this.worldName = assertNotNull(worldName);
    }

    private Stage advanceSync() throws IllegalStateException {
      final WorldInfo info = map.getInfo().getWorld();
      final World world = NMS_HACKS.createWorld(
          worldName, info.getEnvironment(), info.hasTerrain(), info.getSeed());

      if (world == null) throw new IllegalStateException("Unable to load a null world");

      world.setPVP(true);
      world.setSpawnFlags(false, false);
      world.setAutoSave(false);
      world.setDifficulty(difficulties[map.getInfo().getDifficulty()]);

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
    public Duration delay(boolean timedOut) {
      return timedOut ? Duration.ZERO : Duration.ofSeconds(3);
    }
  }

  /** Stage #4: initializes and loads the {@link Match}. */
  private static class InitMatchStage implements Stage, Revertable, Commitable {
    private final Match match;

    private InitMatchStage(World world, MapContext map) {
      this.match =
          new MatchImpl(Long.toString(counter.get() - 1), assertNotNull(map), assertNotNull(world));
    }

    private MoveMatchStage advanceSync() {
      final boolean move = PGM.get().getMatchManager().getMatches().hasNext();
      match.load();
      if (move) {
        return new MoveMatchStage(match);
      } else {
        match.callEvent(new MatchAfterLoadEvent(match));
        return null;
      }
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
    private final int teleportsPerSecond;

    private MoveMatchStage(Match match) {
      this.match = assertNotNull(match);

      // Right before moving players, make sure they don't show up in tab list due to missing a team
      for (Player viewer : Bukkit.getOnlinePlayers()) {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (viewer.canSee(player) && viewer != player) players.add(player.getName());
        }
        String prefix = ChatColor.AQUA.toString();
        TAB_PACKETS
            .teamCreatePacket(DUMMY_TEAM, DUMMY_TEAM, prefix, "", false, false, players)
            .send(viewer);
      }

      int tpPerSecond = Integer.MAX_VALUE;
      try {
        tpPerSecond = TextParser.parseInteger(
            PGM.get()
                .getConfiguration()
                .getExperiments()
                .getOrDefault("match-teleports-per-second", "")
                .toString(),
            Range.atLeast(1));
      } catch (TextException e) {
        // No-op, since an experimental feature
      }
      this.teleportsPerSecond = tpPerSecond;
    }

    private Stage advanceSync() {
      // Create copy to avoid CME on mach unload
      int teleported = 0;
      for (Match oldMatch : Lists.newArrayList(PGM.get().getMatchManager().getMatches())) {
        if (match.equals(oldMatch)) continue;

        for (MatchPlayer player : oldMatch.getPlayers()) {
          if (teleported++ >= teleportsPerSecond) return this;

          final Player bukkit = player.getBukkit();

          oldMatch.removePlayer(bukkit);
          match.addPlayer(bukkit);
        }

        // Old match should be empty. TP or kick anyone still in the old world.
        ensureEmpty(oldMatch.getWorld());

        oldMatch.unload();
      }

      // After all players have been teleported, remove the dummy team
      TAB_PACKETS.teamRemovePacket(DUMMY_TEAM).broadcast();

      match.callEvent(new MatchAfterLoadEvent(match));

      return null;
    }

    private void ensureEmpty(World world) {
      // No one left, we're good
      if (world.getPlayerCount() <= 0) return;

      var spawn = match.moduleRequire(SpawnMatchModule.class).getDefaultSpawn();
      for (Player player : world.getPlayers()) {
        var matchPlayer = match.getPlayer(player);
        Location loc = matchPlayer == null ? null : spawn.getSpawn(matchPlayer);

        if (loc != null) {
          player.teleport(loc);
        } else {
          player.kickPlayer(
              ChatColor.RED + TextTranslations.translate("misc.incorrectWorld", player));
          PGM.get()
              .getLogger()
              .info("Kicked " + player.getName() + " due to not being in the right match");
        }
      }
    }

    @Override
    public Future<? extends Stage> advance() {
      return runMainThread(this::advanceSync);
    }

    @Override
    public Duration delay(boolean timedOut) {
      return Duration.ofSeconds(1);
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
    return CompletableFuture.supplyAsync(() -> {
      try {
        return task.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
