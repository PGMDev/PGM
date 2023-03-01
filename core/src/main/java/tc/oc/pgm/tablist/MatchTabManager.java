package tc.oc.pgm.tablist;

import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.collect.Range;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchResizeEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.concurrent.RateLimiter;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.PlayerTabEntry;
import tc.oc.pgm.util.tablist.TabEntry;
import tc.oc.pgm.util.tablist.TabManager;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public class MatchTabManager extends TabManager implements Listener {

  // Min and max delay for an update after tab-list is invalidated
  private static final int MIN_DELAY = 100, MAX_DELAY = 5_000;
  // How many MS must be waited for each MS spent rendering
  private static final int TIME_RATIO = 40;
  // How many MS must be waited for each TPS under 20 (last minute average)
  private static final int TPS_RATIO = 1000;

  private final Map<Team, TeamTabEntry> teamEntries;
  private final Map<Match, MapTabEntry> mapEntries;
  private final Map<Match, TabEntry[]> legacyHeaderEntries;
  private final Map<Match, MatchFooterTabEntry> footerEntries;
  private final Map<Match, FreeForAllTabEntry> freeForAllEntries;

  private final ScheduledExecutorService executor = PGM.get().getExecutor();

  private Future<?> pingUpdateTask;

  private boolean renderingEnabled = true;
  private RenderTask renderTask;
  private Future<?> renderHeaderFooterTask;
  private final RateLimiter rateLimit =
      new RateLimiter(MIN_DELAY, MAX_DELAY, TIME_RATIO, TPS_RATIO);

  public MatchTabManager(Plugin plugin) {
    this(
        plugin,
        PlayerTabEntry::new,
        TeamTabEntry::new,
        MapTabEntry::new,
        MatchTabManager::headerFactory,
        MatchFooterTabEntry::new,
        FreeForAllTabEntry::new);
  }

  @Deprecated
  // Kept for compatibility until next release
  public MatchTabManager(
      Plugin plugin,
      Function<Player, ? extends PlayerTabEntry> playerProvider,
      Function<Team, ? extends TeamTabEntry> teamProvider,
      Function<Match, ? extends MapTabEntry> headerProvider,
      Function<Match, ? extends MatchFooterTabEntry> footerProvider,
      Function<Match, ? extends FreeForAllTabEntry> ffaProvider) {
    this(
        plugin,
        playerProvider,
        teamProvider,
        headerProvider,
        MatchTabManager::headerFactory,
        footerProvider,
        ffaProvider);
  }

  public MatchTabManager(
      Plugin plugin,
      Function<Player, ? extends PlayerTabEntry> playerProvider,
      Function<Team, ? extends TeamTabEntry> teamProvider,
      Function<Match, ? extends MapTabEntry> headerProvider,
      Function<Match, ? extends TabEntry[]> legacyHeaderProvider,
      Function<Match, ? extends MatchFooterTabEntry> footerProvider,
      Function<Match, ? extends FreeForAllTabEntry> ffaProvider) {
    super(plugin, MatchTabView::new, playerProvider);

    teamEntries = new DefaultMapAdapter<>(teamProvider, true);
    mapEntries = new DefaultMapAdapter<>(headerProvider, true);
    legacyHeaderEntries = new DefaultMapAdapter<>(legacyHeaderProvider, true);
    footerEntries = new DefaultMapAdapter<>(footerProvider, true);
    freeForAllEntries = new DefaultMapAdapter<>(ffaProvider, true);

    if (PGM.get().getConfiguration().showTabListPing()) {
      PlayerTabEntry.setShowRealPing(true);
      // If ping is shown, update all views every 30 seconds like vanilla does
      pingUpdateTask =
          PGM.get().getExecutor().scheduleWithFixedDelay(this::renderPing, 5, 30, TimeUnit.SECONDS);
    } else {
      PlayerTabEntry.setShowRealPing(false);
    }

    PlayerTabEntry.setPlayerComponent(pl -> player(pl, NameStyle.TAB));
  }

  protected static TabEntry[] headerFactory(Match match) {
    return new TabEntry[] {
      new MapTabEntry(match),
      new AuthorTabEntry(match, 0),
      new AuthorTabEntry(match, 1),
      new MatchFooterTabEntry(match)
    };
  }

  public void disable() {
    if (this.renderTask != null) {
      this.renderTask.cancel();
      this.renderTask = null;
    }
    if (this.pingUpdateTask != null) {
      this.pingUpdateTask.cancel(true);
      this.pingUpdateTask = null;
    }

    HandlerList.unregisterAll(this);
  }

  @Override
  protected void scheduleRender() {
    if (dirty.isHeaderOrFooter() && this.renderHeaderFooterTask == null) {
      this.renderHeaderFooterTask =
          executor.schedule(
              () -> {
                this.renderHeaderFooterTask = null;
                this.renderHeaderFooter();
              },
              100,
              TimeUnit.MILLISECONDS);
    }

    // Already a priority task scheduled, nothing to be done
    if (dirty.isLayoutOrContent() && renderingEnabled) {
      if (dirty.isPriority()) {
        if (renderTask == null || !renderTask.isPriority()) {
          cancelScheduledRender();
          this.renderTask = new RenderTask(true, 100);
        }
      } else if (renderTask == null) {
        this.renderTask = new RenderTask(false, rateLimit.getDelay());
      }
    }
  }

  private void cancelScheduledRender() {
    if (renderTask != null) {
      renderTask.cancel();
      this.renderTask = null;
    }
  }

  private void setRenderingEnabled(boolean enabled) {
    if (enabled != renderingEnabled) {
      this.renderingEnabled = enabled;
      if (renderingEnabled) {
        scheduleRender();
      } else {
        cancelScheduledRender();
      }
    }
  }

  @Override
  public @Nullable MatchTabView getViewOrNull(Player viewer) {
    return (MatchTabView) super.getViewOrNull(viewer);
  }

  @Override
  public @Nullable MatchTabView getView(Player viewer) {
    MatchTabView view = (MatchTabView) super.getView(viewer);
    if (view != null) {
      plugin.getServer().getPluginManager().registerEvents(view, PGM.get());
    }
    return view;
  }

  public PlayerTabEntry getPlayerEntry(MatchPlayer player) {
    return (PlayerTabEntry) this.getPlayerEntry(player.getBukkit());
  }

  public TeamTabEntry getTeamEntry(Team team) {
    return this.teamEntries.get(team);
  }

  public FreeForAllTabEntry getFreeForAllEntry(Match match) {
    return this.freeForAllEntries.get(match);
  }

  public MapTabEntry getMapEntry(Match match) {
    return this.mapEntries.get(match);
  }

  public TabEntry[] getHeaderEntries(Match match) {
    return this.legacyHeaderEntries.get(match);
  }

  public MatchFooterTabEntry getFooterEntry(Match match) {
    return this.footerEntries.get(match);
  }

  protected void invalidate(MatchPlayer player) {
    getPlayerEntry(player).invalidate();
    for (Contributor author : player.getMatch().getMap().getAuthors()) {
      if (author.isPlayer(player.getId())) {
        MapTabEntry mapEntry = mapEntries.get(player.getMatch());
        if (mapEntry != null) mapEntry.invalidate();
        break;
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoin(PlayerJoinEvent event) {
    if (ViaUtils.isReady(event.getPlayer())) tryEnable(event.getPlayer());
    else {
      // Player connection hasn't been set up yet, try next tick
      PGM.get()
          .getExecutor()
          .schedule(() -> tryEnable(event.getPlayer()), 50, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Method that will try to enable the view for this player. This can only be done after the player
   * has been successfully injected by via version, if done earlier, it results in 1.7 clients
   * sometimes getting 1.8 tab
   *
   * @param player The player to enable the view for
   */
  private void tryEnable(Player player) {
    if (!player.isOnline()) return;
    MatchTabView view = getViewOrNull(player);
    if (view != null) view.enable(this);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    setRenderingEnabled(false);
    cancelScheduledRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    setRenderingEnabled(true);
    // No regular renders for 5s after cycle, priority renders can still occur
    rateLimit.timeOut(5_000);

    // One priority render scheduled, essentially forcing one full re-render
    enabledViews.forEach(
        (player, view) -> {
          if (view != null) view.getDirtyTracker().prioritize();
        });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchUnload(MatchUnloadEvent event) {
    TeamMatchModule tmm = event.getMatch().getModule(TeamMatchModule.class);
    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        this.teamEntries.remove(team);
      }
    }
    this.freeForAllEntries.remove(event.getMatch());
    this.mapEntries.remove(event.getMatch());
    this.legacyHeaderEntries.remove(event.getMatch());
    this.footerEntries.remove(event.getMatch());
  }

  /** Delegated events */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoinMatch(PlayerJoinMatchEvent event) {
    MatchTabView view = this.getView(event.getPlayer().getBukkit());
    if (view != null) view.onViewerJoinMatch(event);
    invalidate(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeamChange(PlayerPartyChangeEvent event) {
    invalidate(event.getPlayer());

    if (event.getOldParty() instanceof Team) {
      this.getTeamEntry((Team) event.getOldParty()).invalidate();
    }

    if (event.getNewParty() instanceof Team) {
      this.getTeamEntry((Team) event.getNewParty()).invalidate();
    }

    if (event.getOldParty() instanceof Tribute || event.getNewParty() instanceof Tribute) {
      this.getFreeForAllEntry(event.getMatch()).invalidate();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamRename(PartyRenameEvent event) {
    if (event.getParty() instanceof Team) {
      this.getTeamEntry((Team) event.getParty()).invalidate();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamResize(TeamResizeEvent event) {
    this.getTeamEntry(event.getTeam()).invalidate();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchResize(MatchResizeEvent event) {
    FreeForAllTabEntry entry = this.getFreeForAllEntry(event.getMatch());
    if (entry != null) entry.invalidate();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDeath(MatchPlayerDeathEvent event) {
    invalidate(event.getVictim());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSpawn(ParticipantSpawnEvent event) {
    invalidate(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onVanish(PlayerVanishEvent event) {
    invalidate(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerNameChange(NameDecorationChangeEvent event) {
    TabEntry entry = getPlayerEntryOrNull(Bukkit.getPlayer(event.getUUID()));
    if (entry instanceof DynamicTabEntry) ((DynamicTabEntry) entry).invalidate();
  }

  private Integer getRenderBatchSize() {
    try {
      Map<String, Object> experiments = PGM.get().getConfiguration().getExperiments();

      Object value = experiments.get("tablist-batch-size");
      if (value == null) return null;

      return TextParser.parseInteger(value.toString(), Range.atLeast(1));
    } catch (TextException t) {
      return null;
    }
  }

  private class RenderTask implements Runnable {

    private final boolean priority;
    private final Future<?> future;

    public RenderTask(boolean priority, long millis) {
      this.priority = priority;
      this.future = executor.schedule(this, millis, TimeUnit.MILLISECONDS);
    }

    public boolean isPriority() {
      return priority;
    }

    public void cancel() {
      this.future.cancel(true);
    }

    @Override
    public void run() {
      renderTask = null;
      if (priority) {
        priorityRender();
      } else {
        rateLimit.beforeTask();
        Integer batchSize = getRenderBatchSize();
        if (batchSize != null) partialRender(batchSize);
        else render();
        rateLimit.afterTask();
      }

      // Try to re-schedule if anything is still dirty
      if (dirty.isDirty()) scheduleRender();
    }
  }
}
