package tc.oc.pgm.scoreboard;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.joda.time.Duration;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedText;
import tc.oc.named.NameStyle;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchVictoryChangeEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.party.event.PartyAddEvent;
import tc.oc.pgm.api.party.event.PartyRemoveEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.events.FeatureChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.events.TeamRespawnsChangeEvent;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;
import tc.oc.server.NullCommandSender;

@ListenerScope(MatchScope.LOADED)
public class SidebarMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<SidebarMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return ImmutableList.of(ScoreboardMatchModule.class);
    }

    @Override
    public SidebarMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new SidebarMatchModule(match);
    }
  }

  public static final int MAX_ROWS = 16; // Max rows on the scoreboard
  public static final int MAX_PREFIX = 16; // Max chars in a team prefix
  public static final int MAX_SUFFIX = 16; // Max chars in a team suffix

  protected final Map<Party, Sidebar> sidebars = new HashMap<>();
  protected final Map<Goal, BlinkTask> blinkingGoals = new HashMap<>();

  protected @Nullable BukkitTask renderTask;

  private static String renderSidebarTitle(Collection<MapTag> tags) {
    final List<String> gamemode =
        tags.stream()
            .filter(MapTag::isGamemode)
            .filter(tag -> !tag.isAuxiliary())
            .map(MapTag::getName)
            .collect(Collectors.toList());
    final List<String> auxiliary =
        tags.stream()
            .filter(MapTag::isGamemode)
            .filter(MapTag::isAuxiliary)
            .map(MapTag::getName)
            .collect(Collectors.toList());

    String title = "";

    if (gamemode.size() == 1) {
      title = gamemode.get(0);
    } else if (gamemode.size() >= 2) {
      title = "Objectives";
    }

    if (auxiliary.size() == 1) {
      title += (title.isEmpty() ? "" : "& ") + auxiliary.get(0);
    } else if (gamemode.isEmpty() && auxiliary.size() == 2) {
      title = auxiliary.get(0) + " & " + auxiliary.get(1);
    }

    return title.isEmpty() ? "Match" : title;
  }

  private class Sidebar {

    private static final String IDENTIFIER = "pgm";

    private final Scoreboard scoreboard;
    private final Objective objective;

    // Each row has its own scoreboard team
    protected final String[] rows = new String[MAX_ROWS];
    protected final int[] scores = new int[MAX_ROWS];
    protected final Team[] teams = new Team[MAX_ROWS];
    protected final String[] players = new String[MAX_ROWS];

    private Sidebar(Party party) {
      this.scoreboard = match.needModule(ScoreboardMatchModule.class).getScoreboard(party);
      this.objective = this.scoreboard.registerNewObjective(IDENTIFIER, "dummy");
      this.objective.setDisplayName(
          StringUtils.left(
              ComponentRenderers.toLegacyText(
                  new PersonalizedText(
                      renderSidebarTitle(match.getMap().getTags()),
                      net.md_5.bungee.api.ChatColor.AQUA),
                  NullCommandSender.INSTANCE),
              32));
      this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

      for (int i = 0; i < MAX_ROWS; ++i) {
        this.rows[i] = null;
        this.scores[i] = -1;

        this.players[i] = String.valueOf(ChatColor.COLOR_CHAR) + (char) i;

        this.teams[i] = this.scoreboard.registerNewTeam(IDENTIFIER + "-row-" + i);
        this.teams[i].setPrefix("");
        this.teams[i].setSuffix("");
        this.teams[i].addEntry(this.players[i]);
      }
    }

    public Scoreboard getScoreboard() {
      return this.scoreboard;
    }

    public Objective getObjective() {
      return this.objective;
    }

    private void setRow(int maxScore, int row, @Nullable String text) {
      if (row < 0 || row >= MAX_ROWS) return;

      int score = text == null ? -1 : maxScore - row - 1;
      if (this.scores[row] != score) {
        this.scores[row] = score;

        if (score == -1) {
          this.scoreboard.resetScores(this.players[row]);
        } else {
          this.objective.getScore(this.players[row]).setScore(score);
        }
      }

      if (!Objects.equals(this.rows[row], text)) {
        this.rows[row] = text;

        if (text != null) {
          /*
           Split the row text into prefix and suffix, limited to 16 chars each. Because the player name
           is a color code, we have to restore the color at the split in the suffix. We also have to be
           careful not to split in the middle of a color code.
          */
          int split = MAX_PREFIX - 1; // Start by assuming there is a color code right on the split
          if (text.length() < MAX_PREFIX || text.charAt(split) != ChatColor.COLOR_CHAR) {
            // If there isn't, we can fit one more char in the prefix
            split++;
          }

          // Split and truncate the text, and restore the color in the suffix
          String prefix = StringUtils.substring(text, 0, split);
          String lastColors = ChatColor.getLastColors(prefix);
          String suffix =
              lastColors
                  + StringUtils.substring(text, split, split + MAX_SUFFIX - lastColors.length());
          this.teams[row].setPrefix(prefix);
          this.teams[row].setSuffix(suffix);
        }
      }
    }
  }

  private final Match match;

  public SidebarMatchModule(Match match) {
    this.match = match;
  }

  private boolean hasScores() {
    return match.getModule(ScoreMatchModule.class) != null;
  }

  private boolean isBlitz() {
    return match.getModule(BlitzMatchModule.class) != null;
  }

  private boolean isCompactWool() {
    WoolMatchModule wmm = match.getModule(WoolMatchModule.class);
    return wmm != null
        && MAX_ROWS < wmm.getWools().keySet().size() * 2 - 1 + wmm.getWools().values().size();
  }

  private void addSidebar(Party party) {
    match.getLogger().fine("Adding sidebar for party " + party);
    sidebars.put(party, new Sidebar(party));
  }

  @Override
  public void load() {
    for (Party party : match.getParties()) addSidebar(party);
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

  @EventHandler
  public void addParty(PartyAddEvent event) {
    addSidebar(event.getParty());
    renderSidebarDebounce();
  }

  @EventHandler
  public void removeParty(PartyRemoveEvent event) {
    match.getLogger().fine("Removing sidebar for party " + event.getParty());
    sidebars.remove(event.getParty());
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
      blinkGoal(event.getGoal(), 3, Duration.standardSeconds(1));
    } else {
      renderSidebarDebounce();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalProximityChange(final GoalProximityChangeEvent event) {
    if (Config.Scoreboard.showProximity()) {
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

  private String renderGoal(Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    StringBuilder sb = new StringBuilder(" ");

    BlinkTask blinkTask = this.blinkingGoals.get(goal);
    if (blinkTask != null && blinkTask.isDark()) {
      sb.append(ChatColor.BLACK);
    } else {
      sb.append(goal.renderSidebarStatusColor(competitor, viewingParty));
    }
    sb.append(goal.renderSidebarStatusText(competitor, viewingParty));

    if (goal instanceof ProximityGoal) {
      sb.append(" ");
      // Show teams their own proximity on shared goals
      sb.append(((ProximityGoal) goal).renderProximity(competitor, viewingParty));
    }

    sb.append(" ");
    sb.append(goal.renderSidebarLabelColor(competitor, viewingParty));
    sb.append(goal.renderSidebarLabelText(competitor, viewingParty));

    return sb.toString();
  }

  private String renderScore(Competitor competitor, Party viewingParty) {
    ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
    String text = ChatColor.WHITE.toString() + (int) smm.getScore(competitor);
    if (smm.hasScoreLimit()) {
      text += ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + smm.getScoreLimit();
    }
    return text;
  }

  private String renderBlitz(Competitor competitor, Party viewingParty) {
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
    if (this.renderTask == null
        || !match.getScheduler(MatchScope.LOADED).isPending(this.renderTask)) {
      this.renderTask =
          match
              .getScheduler(MatchScope.LOADED)
              .runTask(
                  new Runnable() {
                    @Override
                    public void run() {
                      SidebarMatchModule.this.renderTask = null;
                      SidebarMatchModule.this.renderSidebar();
                    }
                  });
    }
  }

  private void renderSidebar() {
    final boolean hasScores = hasScores();
    final boolean isBlitz = isBlitz();
    final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

    Set<Competitor> competitorsWithGoals = new HashSet<>();
    List<Goal> sharedGoals = new ArrayList<>();

    // Count the rows used for goals
    for (Goal goal : gmm.getGoals()) {
      if (goal.isVisible()) {
        if (goal.isShared()) {
          sharedGoals.add(goal);
        } else {
          for (Competitor competitor : gmm.getCompetitors(goal)) {
            competitorsWithGoals.add(competitor);
          }
        }
      }
    }

    for (Map.Entry<Party, Sidebar> entry : this.sidebars.entrySet()) {
      Party viewingParty = entry.getKey();
      Sidebar sidebar = entry.getValue();

      List<String> rows = new ArrayList<>(MAX_ROWS);

      // Scores/Blitz
      if (hasScores || isBlitz) {
        for (Competitor competitor : match.getCompetitors()) {
          String text;
          if (hasScores) {
            text = renderScore(competitor, viewingParty);
          } else {
            text = renderBlitz(competitor, viewingParty);
          }
          if (text.length() != 0) text += " ";
          rows.add(text + competitor.getStyledName(NameStyle.FANCY).toLegacyText());
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
        rows.add(this.renderGoal(goal, null, viewingParty));
      }

      // Team-specific goals
      List<Competitor> sortedCompetitors = new ArrayList<>(competitorsWithGoals);
      if (viewingParty instanceof Competitor) {
        // Participants see competitors in arbitrary order, with their own at the top
        Collections.sort(sortedCompetitors, Ordering.arbitrary());

        // Bump viewing party to the top of the list
        if (sortedCompetitors.remove(viewingParty)) {
          sortedCompetitors.add(0, (Competitor) viewingParty);
        }
      } else {
        // Observers see the competitors sorted by closeness to winning
        // FIXME: Collections.sort(sortedCompetitors, match.getCompetitorRanking());
      }

      for (Competitor competitor : sortedCompetitors) {
        if (!firstTeam) {
          // Add a blank row between teams
          rows.add("");
        }
        firstTeam = false;

        // Add a row for the team name
        rows.add(competitor.getStyledName(NameStyle.FANCY).toLegacyText());

        if (isCompactWool()) {
          String woolText = " ";
          boolean firstWool = true;

          List<Goal> sortedWools = new ArrayList<>(gmm.getGoals(competitor));
          Collections.sort(
              sortedWools,
              new Comparator<Goal>() {
                @Override
                public int compare(Goal a, Goal b) {
                  return a.getName().compareToIgnoreCase(b.getName());
                }
              });

          for (Goal goal : sortedWools) {
            if (goal instanceof MonumentWool && goal.isVisible()) {
              MonumentWool wool = (MonumentWool) goal;
              if (!firstWool) {
                woolText += "   ";
              }
              firstWool = false;
              woolText += wool.renderSidebarStatusColor(competitor, viewingParty);
              woolText += wool.renderSidebarStatusText(competitor, viewingParty);
            }
          }

          rows.add(woolText);

        } else {
          // Add a row for each of this team's goals
          for (Goal goal : gmm.getGoals()) {
            if (!goal.isShared() && goal.canComplete(competitor) && goal.isVisible()) {
              rows.add(this.renderGoal(goal, competitor, viewingParty));
            }
          }
        }
      }

      if (Config.SidebarMessage.topEnabled()) {
        if (rows.size() < MAX_ROWS || Config.SidebarMessage.overwriteExisting()) {
          rows.add(0, Config.SidebarMessage.formatTop());
        }
      }

      if (Config.SidebarMessage.bottomEnabled()) {
        if (rows.size() < MAX_ROWS - 2) {
          rows.add("");
          rows.add(Config.SidebarMessage.formatBottom());
        } else if (Config.SidebarMessage.overwriteExisting()) {
          rows.set(MAX_ROWS - 2, Config.SidebarMessage.formatBottom());
        }
      }

      // Need at least one row for the sidebar to show
      if (rows.isEmpty()) {
        rows.add("");
      }

      for (int i = 0; i < MAX_ROWS; i++) {
        if (i < rows.size()) {
          sidebar.setRow(rows.size(), i, rows.get(i));
        } else {
          sidebar.setRow(rows.size(), i, null);
        }
      }
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

    private final BukkitTask task;
    private final Goal goal;
    private final long intervalTicks;

    private boolean dark;
    private Long ticksRemaining;

    private BlinkTask(Goal goal, float rateHz, @Nullable Duration duration) {
      this.goal = goal;
      this.intervalTicks = (long) (10f / rateHz);
      this.task = match.getScheduler(MatchScope.RUNNING).runTaskTimer(0, intervalTicks, this);

      this.reset(duration);
    }

    public void reset(@Nullable Duration duration) {
      this.ticksRemaining = duration == null ? null : duration.getMillis() / 50;
    }

    public void stop() {
      this.task.cancel();
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
          this.task.cancel();
          SidebarMatchModule.this.blinkingGoals.remove(this.goal);
        }
      }

      this.dark = !this.dark;
      renderSidebarDebounce();
    }
  }
}
