package tc.oc.pgm.tablist;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.tablist.TabDisplay;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.util.text.types.PlayerComponent;

/** 1.7 legacy tablist implementation */
public class LegacyMatchTabDisplay implements Listener {

  private static final int WIDTH = 3;

  // Maximum players that can be listed per team in non-compact mode, which is
  // the full height minus the header, blank row, and team header.
  private static final int MAX_TEAM_SIZE = TabDisplay.HEIGHT - 3;

  // Number of players below the max before changing back to non-compact mode
  private static final int COMPACT_MODE_HYSTERESIS = 2;

  private final TabDisplay tabDisplay;
  private Future<?> timeUpdateTask;
  private Future<?> deferredRenderTask;

  // True: use all columns as a single list of all players
  // False: use a full column for each team
  // For multi match support, this should be saved per-match
  private boolean compact;
  private Match match;

  public LegacyMatchTabDisplay(Plugin plugin) {
    this.tabDisplay = new TabDisplay(plugin, WIDTH);

    this.timeUpdateTask =
        PGM.get().getExecutor().scheduleWithFixedDelay(this::renderTime, 0, 1, TimeUnit.SECONDS);
  }

  public void disable() {
    if (this.deferredRenderTask != null) {
      this.deferredRenderTask.cancel(true);
      this.deferredRenderTask = null;
    }

    this.timeUpdateTask.cancel(true);
    this.timeUpdateTask = null;

    HandlerList.unregisterAll(this);
    this.tabDisplay.disable();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (ViaUtils.isReady(event.getPlayer())) tryEnable(event.getPlayer());
    else {
      // Player connection hasn't been setup yet, try next tick
      PGM.get()
          .getExecutor()
          .schedule(() -> tryEnable(event.getPlayer()), 50, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Method that will try to enable the display for this player. This can only be done after the
   * player has been successfully injected by via version, if done earlier, it results in 1.7
   * clients sometimes not getting the 1.7 tab
   *
   * @param player The player to enable the display for
   */
  private void tryEnable(Player player) {
    if (!player.isOnline()) return;
    if (ViaUtils.getProtocolVersion(player) >= ViaUtils.VERSION_1_8) return;
    this.tabDisplay.addViewer(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (PGM.get().getMatchManager().getMatch(event.getPlayer()) != null) {
      this.deferredRender();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoinMatch(final PlayerJoinMatchEvent event) {
    if (event.getMatch() != null) this.match = event.getMatch();
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeamChange(PlayerPartyChangeEvent event) {
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamRename(PartyRenameEvent event) {
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamResize(TeamResizeEvent event) {
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerVanish(PlayerVanishEvent event) {
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerNameChange(NameDecorationChangeEvent event) {
    this.deferredRender();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player != null && this.tabDisplay.getViewers().contains(player.getBukkit())) {
      this.render(player);
    }
  }

  private void deferredRender() {
    if (this.deferredRenderTask != null) return;

    // Render one tick later so the effects of the event are visible, and so multiple updates are
    // batched
    this.deferredRenderTask =
        PGM.get().getExecutor().schedule((Runnable) this::render, 50, TimeUnit.MILLISECONDS);
  }

  /** Re-render the whole tab for all players using this display */
  private void render() {
    checkCompactMode(this.match);
    for (Player viewer : tabDisplay.getViewers()) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(viewer);
      if (matchPlayer == null) continue; // Player isn't in a match

      render(matchPlayer);
    }
    deferredRenderTask = null;
  }

  private void checkCompactMode(Match match) {
    int largestTeam = 0;
    for (Competitor team : match.getCompetitors()) {
      if (team.getPlayers().size() > largestTeam) {
        largestTeam = team.getPlayers().size();
      }
    }

    // Have a margin between switching modes, so it doesn't flicker back and forth
    this.compact =
        match.getCompetitors().size() > WIDTH
            || largestTeam > MAX_TEAM_SIZE
            || (this.compact && largestTeam > MAX_TEAM_SIZE - COMPACT_MODE_HYSTERESIS);
  }

  private void render(MatchPlayer viewer) {
    if (!viewer.isLegacy()) return;

    Player bukkit = viewer.getBukkit();
    MapInfo mapInfo = viewer.getMatch().getMap();

    // Show map name in the top left corner, taking a very rough guess at the length limit.
    // We could maybe use ChatUtils.pixelWidth() in the future for more accuracy.
    String mapName = mapInfo.getName();
    if (mapName.length() > 15) {
      mapName = mapName.substring(0, 14) + "...";
    }
    this.tabDisplay.set(bukkit, 0, 0, ChatColor.AQUA + mapName);

    // If there is exactly one map author, show their name in the top middle slot.
    // Multiple names will surely not fit, so showing none of them is the only fair thing.
    if (mapInfo.getAuthors().size() == 1) {
      String author = mapInfo.getAuthors().iterator().next().getNameLegacy();
      this.tabDisplay.set(
          bukkit,
          1,
          0,
          TextTranslations.translateLegacy(
              TranslatableComponent.of(
                  "misc.by",
                  TextColor.DARK_GRAY,
                  (author == null)
                      ? TranslatableComponent.of(
                          "misc.unknown", TextColor.GRAY, TextDecoration.ITALIC)
                      : TextComponent.of(author, TextColor.GRAY)),
              bukkit));
    } else {
      this.tabDisplay.set(
          bukkit,
          1,
          0,
          TextTranslations.translateLegacy(
              TranslatableComponent.of(
                  "map.info.commandHint",
                  TextColor.DARK_GRAY,
                  TextComponent.of("/map", TextColor.GRAY)),
              bukkit));
    }

    // Time in top right corner
    this.renderTime(viewer);

    for (int x = 0; x < this.tabDisplay.getWidth(); ++x) {
      this.tabDisplay.set(bukkit, x, 1, "");
    }

    // Use the rest of the rows for teams
    this.renderTeams(viewer, 2, TabDisplay.HEIGHT);
  }

  /** Re-render time for all players, ran once every second */
  private void renderTime() {
    for (Player viewer : tabDisplay.getViewers()) {
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(viewer);
      if (matchPlayer != null) renderTime(matchPlayer);
    }
  }

  private void renderTime(MatchPlayer viewer) {
    TextComponent.Builder content =
        TextComponent.builder()
            .append(TranslatableComponent.of("match.info.time", TextColor.GRAY))
            .append(": ", TextColor.GRAY)
            .append(
                TimeUtils.formatDuration(viewer.getMatch().getDuration()),
                viewer.getMatch().isRunning() ? TextColor.GREEN : TextColor.GOLD);

    this.tabDisplay.set(
        viewer.getBukkit(),
        this.tabDisplay.getWidth() - 1,
        0,
        TextTranslations.translateLegacy(content.build(), viewer.getBukkit()));
  }

  private void renderTeams(final MatchPlayer viewer, int yMin, int yMax) {
    Match match = viewer.getMatch();
    int columns = this.tabDisplay.getWidth();
    int rows = yMax - yMin;

    // Build a list of teams in the order we want to show them
    List<Party> teams = new ArrayList<>();

    // If the viewer has joined the match, show their team at the top
    if (viewer.getParty() instanceof Competitor) {
      teams.add(viewer.getParty());
    }

    // Followed by the other participating teams
    for (Competitor team : match.getCompetitors()) {
      if (team != viewer.getCompetitor()) {
        teams.add(team);
      }
    }

    // And the observers at the bottom
    if (PGM.get().getConfiguration().canParticipantsSeeObservers()
        || viewer.getParty() instanceof ObservingParty) {
      teams.add(match.getDefaultParty());
    }

    TreeSet<MatchPlayer> players = new TreeSet<>(new PlayerOrder(viewer));

    if (this.compact || isFFA(viewer.getMatch())) {
      // Render a single list that spans all columns
      final int participatingTeams = teams.size() - viewer.getMatch().getObservers().size();
      final int totalSlots = columns * rows;
      int currentSlot = 0;

      // First, all the team names/player counts
      if (isFFA(viewer.getMatch())) {
        this.tabDisplay.set(
            viewer.getBukkit(),
            0,
            yMin + currentSlot,
            match.getCompetitors().size()
                + " "
                + TextTranslations.translateLegacy(
                    TranslatableComponent.of(
                        "match.info.players", TextColor.YELLOW, TextDecoration.BOLD),
                    viewer.getBukkit()));
        currentSlot++;
      }
      for (Party team : teams) {
        if (renderTeamName(viewer, team, 0, yMin + currentSlot)) currentSlot++;
      }

      // Blank slot
      this.tabDisplay.set(viewer.getBukkit(), 0, yMin + currentSlot, "");
      currentSlot++;

      // All players, sorted by team and then by PlayerOrder
      for (int i = 0; i < teams.size(); ++i) {
        Party team = teams.get(i);
        players.clear();
        players.addAll(team.getPlayers());

        // If the list overflows, allocate the space fairly between teams, but prioritize
        // participating teams
        final int remainingTeams;
        if (i < participatingTeams) {
          remainingTeams = participatingTeams - i;
        } else {
          remainingTeams = teams.size() - i;
        }
        final int limit =
            currentSlot + Math.min(players.size(), (totalSlots - currentSlot) / remainingTeams);

        for (MatchPlayer player : players) {
          if (currentSlot >= limit) {
            break;
          }
          if (renderPlayerName(viewer, player, currentSlot / rows, yMin + currentSlot % rows))
            currentSlot++;
        }
      }

      // Fill in the trailing slots
      for (; currentSlot < totalSlots; ++currentSlot) {
        this.tabDisplay.set(viewer.getBukkit(), currentSlot / rows, yMin + currentSlot % rows, "");
      }
    } else {
      // Render a team in each column
      for (int x = 0; x < columns; x++) {
        int y = yMin;

        if (x < teams.size()) {
          Party team = teams.get(x);

          if (renderTeamName(viewer, team, x, y)) y++;

          players.clear();
          players.addAll(team.getPlayers());

          for (MatchPlayer player : players) {
            if (y >= yMax) {
              break; // Should never happen because of compact mode
            }
            if (renderPlayerName(viewer, player, x, y)) y++;
          }
        }

        // Fill in the trailing slots
        for (; y < yMax; ++y) {
          this.tabDisplay.set(viewer.getBukkit(), x, y, "");
        }
      }
    }
  }

  protected boolean renderPlayerName(MatchPlayer viewer, MatchPlayer player, int x, int y) {
    if (!isVisible(player, viewer)) return false;

    this.tabDisplay.set(viewer.getBukkit(), x, y, getPlayerName(viewer, player));
    return true;
  }

  private boolean isVisible(MatchPlayer target, MatchPlayer viewer) {
    if (viewer.getBukkit().hasPermission(Permissions.STAFF)) return true;
    return !PGM.get().getVanishManager().isVanished(target.getId());
  }

  private boolean isFFA(Match match) {
    return match.getModule(FreeForAllMatchModule.class) != null;
  }

  protected boolean renderTeamName(MatchPlayer viewer, Party party, int x, int y) {
    if (party instanceof Tribute) return false; // Avoid rendering FFA

    this.tabDisplay.set(viewer.getBukkit(), x, y, getTeamName(viewer, party));
    return true;
  }

  protected String getPlayerName(MatchPlayer viewer, MatchPlayer player) {
    return TextTranslations.translateLegacy(
        PlayerComponent.of(player.getBukkit(), NameStyle.LEGACY_TAB, viewer.getBukkit()),
        viewer.getBukkit());
  }

  protected String getTeamName(MatchPlayer viewer, Party party) {
    String playerCount =
        ChatColor.WHITE.toString()
            + party.getPlayers().stream().filter(pl -> isVisible(pl, viewer)).count();
    if (party instanceof Team) {
      Team team = (Team) party;
      playerCount += ChatColor.DARK_GRAY.toString() + "/" + ChatColor.GRAY + team.getMaxPlayers();
    }
    String name = party.getNameLegacy();
    if (name.toLowerCase().endsWith(" team")) {
      name = name.substring(0, name.length() - " team".length());
    }
    return playerCount + " " + party.getColor().toString() + ChatColor.BOLD + name;
  }
}
