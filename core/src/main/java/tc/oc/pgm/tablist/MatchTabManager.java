package tc.oc.pgm.tablist;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchResizeEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.PlayerTabEntry;
import tc.oc.pgm.util.tablist.TabEntry;
import tc.oc.pgm.util.tablist.TabManager;

public class MatchTabManager extends TabManager implements Listener {

  private final Map<Team, TeamTabEntry> teamEntries;
  private final Map<Match, MapTabEntry> mapEntries;
  private final Map<Match, MatchFooterTabEntry> footerEntries;
  private final Map<Match, FreeForAllTabEntry> freeForAllEntries;

  private Future<?> pingUpdateTask;
  private Future<?> renderTask;

  public MatchTabManager(Plugin plugin) {
    this(
        plugin,
        PlayerTabEntry::new,
        TeamTabEntry::new,
        MapTabEntry::new,
        MatchFooterTabEntry::new,
        FreeForAllTabEntry::new);
  }

  public MatchTabManager(
      Plugin plugin,
      Function<Player, ? extends PlayerTabEntry> playerProvider,
      Function<Team, ? extends TeamTabEntry> teamProvider,
      Function<Match, ? extends MapTabEntry> headerProvider,
      Function<Match, ? extends MatchFooterTabEntry> footerProvider,
      Function<Match, ? extends FreeForAllTabEntry> ffaProvider) {
    super(plugin, MatchTabView::new, playerProvider);

    teamEntries = new DefaultMapAdapter<>(teamProvider, true);
    mapEntries = new DefaultMapAdapter<>(headerProvider, true);
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
  }

  public void disable() {
    if (this.renderTask != null) {
      this.renderTask.cancel(true);
      this.renderTask = null;
    }
    if (this.pingUpdateTask != null) {
      this.pingUpdateTask.cancel(true);
      this.pingUpdateTask = null;
    }

    HandlerList.unregisterAll(this);
  }

  @Override
  protected void invalidate() {
    super.invalidate();

    if (this.renderTask == null) {
      Runnable render =
          new Runnable() {
            @Override
            public void run() {
              MatchTabManager.this.renderTask = null;
              MatchTabManager.this.render();
            }
          };
      this.renderTask = PGM.get().getExecutor().schedule(render, 250, TimeUnit.MILLISECONDS);
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
      // Player connection hasn't been setup yet, try next tick
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
    if (view == null || ViaUtils.getProtocolVersion(player) < ViaUtils.VERSION_1_8) return;
    view.enable(this);
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
}
