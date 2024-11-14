package tc.oc.pgm.scoreboard;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import fr.mrmicky.fastboard.FastBoard;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchVictoryChangeEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.events.FeatureChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.events.TeamRespawnsChangeEvent;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.concurrent.RateLimiter;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;

@ListenerScope(MatchScope.LOADED)
public class SidebarMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<SidebarMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getWeakDependencies() {
      return ImmutableList.of(
          GoalMatchModule.class, ScoreMatchModule.class, BlitzMatchModule.class);
    }

    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return ImmutableList.of(FilterMatchModule.class, ScoreboardMatchModule.class);
    }

    @Override
    public SidebarMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return PGM.get().getConfiguration().showSideBar() ? new SidebarMatchModule(match) : null;
    }
  }

  protected final Map<UUID, FastBoard> sidebars = new HashMap<>();
  protected final Map<Goal<?>, BlinkTask> blinkingGoals = new HashMap<>();

  protected @Nullable Future<?> renderTask;
  private final RateLimiter rateLimit = new RateLimiter(50, 1000, 40, 1000);

  private final Match match;
  private final SidebarRenderer renderer;
  private final Component title;

  public SidebarMatchModule(Match match) {
    this.match = match;
    this.renderer = new SidebarRenderer(match, this);
    this.title = renderer.renderTitle();
  }

  private FastBoard addSidebar(MatchPlayer player) {
    FastBoard sidebar = new FastBoard(player.getBukkit()) {
      @Override
      public boolean hasLinesMaxLength() {
        return player.getProtocolVersion() < ViaUtils.VERSION_1_13;
      }
    };
    // Only render the title once, since it does not change during the match.
    sidebar.updateTitle(renderer.renderTitle(title, player));

    sidebars.put(player.getId(), sidebar);
    return sidebar;
  }

  @Override
  public void load() {
    for (MatchPlayer player : match.getPlayers()) {
      addSidebar(player);
    }
    renderSidebarDebounce();

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    match
        .needModule(GoalMatchModule.class)
        .getGoals()
        .forEach(goal -> fmm.onChange(
            Match.class, goal.getScoreboardFilter(), (m, v) -> this.renderSidebarDebounce()));
    match
        .moduleOptional(ScoreMatchModule.class)
        .ifPresent(smm -> fmm.onChange(
            Party.class, smm.getScoreboardFilter(), (p, v) -> this.renderSidebarDebounce()));
    match
        .moduleOptional(BlitzMatchModule.class)
        .ifPresent(bmm -> fmm.onChange(
            Party.class, bmm.getScoreboardFilter(), (p, v) -> this.renderSidebarDebounce()));
  }

  @Override
  public void enable() {
    renderSidebarDebounce();
  }

  @Override
  public void disable() {
    for (BlinkTask task : ImmutableSet.copyOf(this.blinkingGoals.values())) {
      task.stop();
    }
  }

  @Override
  public void unload() {
    this.sidebars.clear();
  }

  @EventHandler
  public void localChange(PlayerLocaleChangeEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler
  public void addPlayer(PlayerJoinMatchEvent event) {
    FastBoard sidebar = addSidebar(event.getPlayer());
    renderSidebarDebounce();
    // After match end we stop updating sidebars for lag reasons.
    // That causes newly joined players to have nothing if we don't explicitly render for them.
    if (match.isFinished()) {
      var player = event.getPlayer();
      var rows = renderer.renderSidebar(player.getParty());
      sidebar.updateLines(Collections2.transform(rows, r -> renderer.renderRow(r, player)));
    }
  }

  @EventHandler
  public void removePlayer(PlayerLeaveMatchEvent event) {
    sidebars.remove(event.getPlayer().getId()).delete();
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDeath(MatchPlayerDeathEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSpawn(ParticipantSpawnEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyRename(final PartyRenameEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void scoreChange(final CompetitorScoreChangeEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalTouch(final GoalTouchEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalStatusChange(final GoalStatusChangeEvent event) {
    if (event.getGoal() instanceof Destroyable
        && ((Destroyable) event.getGoal()).getShowProgress()) {
      blinkGoal(event.getGoal(), 3, Duration.ofSeconds(1));
    } else {
      renderSidebarDebounce();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalProximityChange(final GoalProximityChangeEvent event) {
    boolean willUseProx = match.moduleRequire(TimeLimitMatchModule.class).willUseProximity();
    if (PGM.get().getConfiguration().showProximity(willUseProx)) {
      renderSidebarDebounce();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void timelimitToggle(final CountdownStartEvent event) {
    if (!(event.getCountdown() instanceof TimeLimitCountdown)) return;
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void timelimitToggle(final CountdownCancelEvent event) {
    if (!(event.getCountdown() instanceof TimeLimitCountdown)) return;
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalComplete(final GoalCompleteEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalChange(final FeatureChangeEvent event) {
    if (event.getFeature() instanceof Goal) {
      renderSidebarDebounce();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void updateRespawnLimit(final TeamRespawnsChangeEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void resultChange(MatchVictoryChangeEvent event) {
    renderSidebarDebounce();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void matchEnd(MatchFinishEvent event) {
    renderSidebarDebounce();
    // After match end, timeout rate-limit indefinitely
    rateLimit.timeOut(Integer.MAX_VALUE);
  }

  private void renderSidebarDebounce() {
    // Debounced render
    if (this.renderTask == null || renderTask.isDone()) {
      Runnable render = () -> {
        rateLimit.beforeTask();
        SidebarMatchModule.this.renderTask = null;
        SidebarMatchModule.this.renderSidebar();
        rateLimit.afterTask();
      };

      this.renderTask = match
          .getExecutor(MatchScope.LOADED)
          .schedule(render, rateLimit.getDelay(), TimeUnit.MILLISECONDS);
    }
  }

  private void renderSidebar() {
    Map<Party, List<Component>> cache = new HashMap<>();

    for (MatchPlayer player : this.match.getPlayers()) {
      FastBoard sidebar = this.sidebars.get(player.getId());
      if (sidebar == null) continue;

      List<Component> rows = cache.computeIfAbsent(player.getParty(), renderer::renderSidebar);
      sidebar.updateLines(Collections2.transform(rows, r -> renderer.renderRow(r, player)));
    }
  }

  public void blinkGoal(Goal<?> goal, float rateHz, @Nullable Duration duration) {
    BlinkTask task = this.blinkingGoals.get(goal);
    if (task != null) {
      task.reset(duration);
    } else {
      this.blinkingGoals.put(goal, new BlinkTask(goal, rateHz, duration));
    }
  }

  public void stopBlinkingGoal(Goal<?> goal) {
    BlinkTask task = this.blinkingGoals.remove(goal);
    if (task != null) task.stop();
  }

  protected class BlinkTask implements Runnable {

    private final Future<?> task;
    private final Goal<?> goal;
    private final long intervalTicks;

    private boolean dark;
    private Long ticksRemaining;

    private BlinkTask(Goal<?> goal, float rateHz, @Nullable Duration duration) {
      this.goal = goal;
      this.intervalTicks = (long) (10f / rateHz);
      this.task = match
          .getExecutor(MatchScope.RUNNING)
          .scheduleWithFixedDelay(this, 0, intervalTicks * TimeUtils.TICK, TimeUnit.MILLISECONDS);

      this.reset(duration);
    }

    public void reset(@Nullable Duration duration) {
      this.ticksRemaining = duration == null ? null : TimeUtils.toTicks(duration);
    }

    public void stop() {
      this.task.cancel(true);
      SidebarMatchModule.this.blinkingGoals.remove(this.goal);
      renderSidebarDebounce();
    }

    public boolean isDark() {
      return this.dark;
    }

    @Override
    public void run() {
      if (this.ticksRemaining != null) {
        this.ticksRemaining -= this.intervalTicks;
        if (this.ticksRemaining <= 0) {
          this.task.cancel(true);
          SidebarMatchModule.this.blinkingGoals.remove(this.goal);
        }
      }

      this.dark = !this.dark;
      renderSidebarDebounce();
    }
  }
}
