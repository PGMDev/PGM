package tc.oc.pgm.tablist;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
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

  private final Map<Team, TeamTabEntry> teamEntries =
      new DefaultMapAdapter<>(TeamTabEntry::new, true);
  private final Map<Match, MapTabEntry> mapEntries =
      new DefaultMapAdapter<>(MapTabEntry::new, true);
  private final Map<Match, MatchFooterTabEntry> footerEntries =
      new DefaultMapAdapter<>(MatchFooterTabEntry::new, true);
  private final Map<Match, FreeForAllTabEntry> freeForAllEntries =
      new DefaultMapAdapter<>(FreeForAllTabEntry::new, true);

  private Future<?> pingUpdateTask;
  private Future<?> renderTask;

  public MatchTabManager(Plugin plugin) {
    super(plugin, MatchTabView::new, null);

    if (PGM.get().getConfiguration().showTabListPing()) {
      PlayerTabEntry.setShowRealPing(true);
      // If ping is shown, invalidate player entries to force-update them every so often
      pingUpdateTask =
          PGM.get()
              .getExecutor()
              .scheduleWithFixedDelay(this::invalidatePlayers, 5, 15, TimeUnit.SECONDS);
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
      this.renderTask = PGM.get().getExecutor().schedule(render, 1, TimeUnit.SECONDS);
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

  /** Invalidates all player entries, used for ping to update */
  private void invalidatePlayers() {
    for (TabEntry value : playerEntries.values()) {
      if (value instanceof DynamicTabEntry) ((DynamicTabEntry) value).invalidate();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoin(PlayerJoinEvent event) {
    if (ViaUtils.getProtocolVersion(event.getPlayer()) <= ViaUtils.VERSION_1_7) return;
    MatchTabView view = this.getView(event.getPlayer());
    if (view != null) view.enable(this);
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
    if (ViaUtils.getProtocolVersion(event.getPlayer().getBukkit()) <= ViaUtils.VERSION_1_7) return;
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
    PlayerTabEntry entry = getPlayerEntry(event.getPlayer());
    entry.invalidate();
    entry.refresh();
  }
}
