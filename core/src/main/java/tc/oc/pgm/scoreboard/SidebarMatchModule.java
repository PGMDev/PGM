package tc.oc.pgm.scoreboard;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import fr.mrmicky.fastboard.FastBoard;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.goal.Goal;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchVictoryChangeEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.events.FeatureChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.events.TeamRespawnsChangeEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

@ListenerScope(MatchScope.LOADED)
public class SidebarMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<SidebarMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return ImmutableList.of(ScoreboardMatchModule.class);
    }

    @Override
    public SidebarMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return PGM.get().getConfiguration().showSideBar() ? new SidebarMatchModule(match) : null;
    }
  }

  public static final int MAX_ROWS = 16; // Max rows on the scoreboard
  public static final int MAX_LENGTH = 30; // Max characters per line allowed
  public static final int MAX_TITLE = 32; // Max characters allowed in title

  protected final Map<UUID, FastBoard> sidebars = new HashMap<>();
  protected final Map<Goal, BlinkTask> blinkingGoals = new HashMap<>();

  protected @Nullable Future<?> renderTask;

  private final Match match;
  private final Component title;

  public SidebarMatchModule(Match match) {
    this.match = match;
    this.title = renderTitle(PGM.get().getConfiguration(), match.getMap());
  }

  private boolean hasScores() {
    return match.getModule(ScoreMatchModule.class) != null;
  }

  private boolean isBlitz() {
    return match.getModule(BlitzMatchModule.class) != null;
  }

  // Determines if wool objectives should be given their own rows, or all shown on 1 row.
  private boolean isCompactWool() {
    WoolMatchModule wmm = match.getModule(WoolMatchModule.class);
    return wmm != null
        && !(wmm.getWools().keySet().size() * 2 - 1 + wmm.getWools().values().size() < MAX_ROWS);
  }

  // Determines if all the map objectives can fit onto the scoreboard with empty rows in between.
  private boolean isSuperCompact(Set<Competitor> competitorsWithGoals) {
    int rowsUsed = competitorsWithGoals.size() * 2 - 1;

    if (isCompactWool()) {
      WoolMatchModule wmm = match.needModule(WoolMatchModule.class);
      rowsUsed += wmm.getWools().keySet().size();
    } else {
      GoalMatchModule gmm = match.needModule(GoalMatchModule.class);
      rowsUsed += gmm.getGoals().size();
    }

    return !(rowsUsed < MAX_ROWS);
  }

  private void addSidebar(MatchPlayer player) {
    sidebars.put(player.getId(), new FastBoard(player.getBukkit()));
  }

  @Override
  public void load() {
    for (MatchPlayer player : match.getPlayers()) {
      addSidebar(player);
    }
    renderSidebarDebounce();
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
    addSidebar(event.getPlayer());
    renderSidebarDebounce();
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
    if (PGM.get().getConfiguration().showProximity()) {
      renderSidebarDebounce();
    }
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
  }

  private Component renderTitle(final Config config, final MapInfo map) {
    final Component header = config.getMatchHeader();
    if (header != null) {
      return header.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Component gamemode = map.getGamemode();
    if (gamemode != null) {
      return gamemode.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Collection<Gamemode> gamemodes = map.getGamemodes();
    if (!gamemodes.isEmpty()) {
      String suffix = gamemodes.size() <= 1 ? ".name" : ".acronym";
      List<Component> gmComponents =
          gamemodes.stream()
              .map(gm -> translatable("gamemode." + gm.getId() + suffix))
              .collect(Collectors.toList());
      return TextFormatter.list(gmComponents, NamedTextColor.AQUA);
    }

    final List<Component> games = new LinkedList<>();

    // First, find a primary game mode
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || tag.isAuxiliary()) continue;

      if (games.isEmpty()) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
        continue;
      }

      // When there are multiple, primary game modes
      games.set(0, translatable("gamemode.generic.name", NamedTextColor.AQUA));
      break;
    }

    // Second, append auxiliary game modes
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || !tag.isAuxiliary()) continue;

      // There can only be 2 game modes
      if (games.size() < 2) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
      } else {
        break;
      }
    }

    return TextFormatter.list(games, NamedTextColor.AQUA);
  }

  private String renderGoal(
      Goal<?> goal, @Nullable Competitor competitor, Party viewingParty, CommandSender viewer) {
    StringBuilder sb = new StringBuilder(" ");

    BlinkTask blinkTask = this.blinkingGoals.get(goal);
    if (blinkTask != null && blinkTask.isDark()) {
      sb.append(ChatColor.BLACK);
    } else {
      sb.append(goal.renderSidebarStatusColor(competitor, viewingParty));
    }
    sb.append(goal.renderSidebarStatusText(competitor, viewingParty));

    if (goal instanceof ProximityGoal) {
      // Show teams their own proximity on shared goals
      String proximity = ((ProximityGoal) goal).renderProximity(competitor, viewingParty);

      if (!proximity.isEmpty()) sb.append(" ").append(proximity);
    }

    sb.append(" ");
    final TextColor color =
        TextFormatter.convert(goal.renderSidebarLabelColor(competitor, viewingParty));
    sb.append(
        TextTranslations.translateLegacy(
            goal.renderSidebarLabelText(competitor, viewingParty).color(color), viewer));

    return sb.toString();
  }

  private String renderScore(Competitor competitor) {
    ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
    String text = ChatColor.WHITE.toString() + (int) smm.getScore(competitor);
    if (smm.hasScoreLimit()) {
      text += ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + smm.getScoreLimit();
    }
    return text;
  }

  private String renderBlitz(Competitor competitor) {
    BlitzMatchModule bmm = match.needModule(BlitzMatchModule.class);
    if (competitor instanceof tc.oc.pgm.teams.Team) {
      return ChatColor.WHITE.toString() + bmm.getRemainingPlayers(competitor);
    } else if (competitor instanceof Tribute && bmm.getConfig().getNumLives() > 1) {
      return ChatColor.WHITE.toString()
          + bmm.getNumOfLives(competitor.getPlayers().iterator().next().getId());
    } else {
      return "";
    }
  }

  private void renderSidebarDebounce() {
    // Debounced render
    if (this.renderTask == null || renderTask.isDone()) {
      this.renderTask =
          match
              .getExecutor(MatchScope.LOADED)
              .submit(
                  () -> {
                    this.renderTask = null;
                    this.renderSidebar();
                  });
    }
  }

  private void renderSidebar() {
    final boolean hasScores = hasScores();
    final boolean isBlitz = isBlitz();
    final boolean isCompactWool = isCompactWool();
    final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

    Set<Competitor> competitorsWithGoals = new HashSet<>();
    List<Goal<?>> sharedGoals = new ArrayList<>();

    // Count the rows used for goals
    for (Goal<?> goal : gmm.getGoals()) {
      if (goal.isVisible()) {
        if (goal.isShared()) {
          sharedGoals.add(goal);
        } else {
          competitorsWithGoals.addAll(gmm.getCompetitors(goal));
        }
      }
    }
    final boolean isSuperCompact = isSuperCompact(competitorsWithGoals);

    for (MatchPlayer player : this.match.getPlayers()) {
      final FastBoard sidebar = this.sidebars.get(player.getId());
      if (sidebar == null) continue;

      final Player viewer = player.getBukkit();
      final Party party = player.getParty();
      final List<String> rows = new ArrayList<>(MAX_ROWS);

      // Scores/Blitz
      if (hasScores || isBlitz) {
        for (Competitor competitor : match.getSortedCompetitors()) {
          String text;
          if (hasScores) {
            text = renderScore(competitor);
          } else {
            text = renderBlitz(competitor);
          }
          if (text.length() != 0) text += " ";
          rows.add(text + TextTranslations.translateLegacy(competitor.getName(), viewer));

          // No point rendering more scores, usually seen in FFA
          if (rows.size() >= MAX_ROWS) break;
        }

        if (!competitorsWithGoals.isEmpty() || !sharedGoals.isEmpty()) {
          // Blank row between scores and goals
          rows.add("");
        }
      }

      boolean firstTeam = true;

      // Shared goals i.e. not grouped under a specific team
      for (Goal goal : sharedGoals) {
        firstTeam = false;
        rows.add(this.renderGoal(goal, null, party, viewer));
      }

      // Team-specific goals
      List<Competitor> sortedCompetitors = new ArrayList<>(match.getSortedCompetitors());
      sortedCompetitors.retainAll(competitorsWithGoals);

      if (party instanceof Competitor) {
        // Bump viewing party to the top of the list
        if (sortedCompetitors.remove(party)) {
          sortedCompetitors.add(0, (Competitor) party);
        }
      }

      for (Competitor competitor : sortedCompetitors) {
        // Prevent team name from showing if there isn't space for at least 1 row of its objectives
        if (!(rows.size() + 2 < MAX_ROWS)) break;

        if (!(firstTeam || isSuperCompact)) {
          // Add a blank row between teams
          rows.add("");
        }
        firstTeam = false;

        // Add a row for the team name
        rows.add(TextTranslations.translateLegacy(competitor.getName(), viewer));

        if (isCompactWool) {
          boolean firstWool = true;

          List<Goal> sortedWools = new ArrayList<>(gmm.getGoals(competitor));
          Collections.sort(sortedWools, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

          // Calculate whether having three spaces between each wool would fit on the scoreboard.
          boolean horizontalCompact =
              MAX_LENGTH < (3 * sortedWools.size()) + (3 * (sortedWools.size() - 1)) + 1;
          String woolText = "";
          if (!horizontalCompact) {
            // If there is extra room, add another space to the left of the wools to make them
            // appear more centered.
            woolText += " ";
          }

          for (Goal<?> goal : sortedWools) {
            if (goal instanceof MonumentWool && goal.isVisible()) {
              MonumentWool wool = (MonumentWool) goal;
              woolText += " ";
              if (!firstWool && !horizontalCompact) woolText += "  ";
              firstWool = false;
              woolText += wool.renderSidebarStatusColor(competitor, party);
              woolText += wool.renderSidebarStatusText(competitor, party);
            }
          }
          // Add a row for the compact wools
          rows.add(woolText);

        } else {
          // Not compact; add a row for each of this team's goals
          for (Goal goal : gmm.getGoals()) {
            if (!goal.isShared() && goal.canComplete(competitor) && goal.isVisible()) {
              rows.add(this.renderGoal(goal, competitor, party, viewer));
            }
          }
        }
      }

      final Component footer = PGM.get().getConfiguration().getMatchFooter();
      if (footer != null) {
        // Only shows footer if there are one or two rows available
        if (rows.size() < MAX_ROWS - 2) {
          rows.add("");
        }
        rows.add(TextTranslations.translateLegacy(footer, viewer));
      }

      // Need at least one row for the sidebar to show
      if (rows.isEmpty()) {
        rows.add("");
      }

      // Limit sidebar to MAX_LENGTH characters
      // Avoids FastBoard throwing errors and stopping the rendering
      for (int i = 0; i < rows.size(); i++) {
        if (rows.get(i).length() > MAX_LENGTH) rows.set(i, rows.get(i).substring(0, MAX_LENGTH));
      }

      String titleStr = TextTranslations.translateLegacy(title, viewer);
      sidebar.updateTitle(titleStr.substring(0, Math.min(titleStr.length(), MAX_TITLE)));
      sidebar.updateLines(rows.size() < MAX_ROWS ? rows : rows.subList(0, MAX_ROWS));
    }
  }

  public void blinkGoal(Goal goal, float rateHz, @Nullable Duration duration) {
    BlinkTask task = this.blinkingGoals.get(goal);
    if (task != null) {
      task.reset(duration);
    } else {
      this.blinkingGoals.put(goal, new BlinkTask(goal, rateHz, duration));
    }
  }

  public void stopBlinkingGoal(Goal goal) {
    BlinkTask task = this.blinkingGoals.remove(goal);
    if (task != null) task.stop();
  }

  private class BlinkTask implements Runnable {

    private final Future<?> task;
    private final Goal goal;
    private final long intervalTicks;

    private boolean dark;
    private Long ticksRemaining;

    private BlinkTask(Goal goal, float rateHz, @Nullable Duration duration) {
      this.goal = goal;
      this.intervalTicks = (long) (10f / rateHz);
      this.task =
          match
              .getExecutor(MatchScope.RUNNING)
              .scheduleWithFixedDelay(
                  this, 0, intervalTicks * TimeUtils.TICK, TimeUnit.MILLISECONDS);

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
