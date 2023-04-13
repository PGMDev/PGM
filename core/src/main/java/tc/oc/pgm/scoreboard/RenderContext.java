package tc.oc.pgm.scoreboard;

import static net.kyori.adventure.text.Component.empty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.wool.WoolMatchModule;

class RenderContext {
  public final @NotNull Match match;
  public final @NotNull Party viewer;
  public final boolean hasScores;
  public final boolean isBlitz;
  public final boolean isCompactWool;
  public final GoalMatchModule gmm;
  public final Set<Competitor> competitorsWithGoals;
  public final List<Goal<?>> sharedGoals;
  public final boolean isSuperCompact;

  private final List<Component> rows = new ArrayList<>(SidebarRenderer.MAX_ROWS);

  private boolean addSpace = false;

  public RenderContext(@NotNull Match match, @NotNull Party viewer) {
    this.match = match;
    this.viewer = viewer;
    this.hasScores = match.getModule(ScoreMatchModule.class) != null;
    this.isBlitz = match.getModule(BlitzMatchModule.class) != null;
    this.isCompactWool = isCompactWool();

    this.gmm = match.needModule(GoalMatchModule.class);
    this.competitorsWithGoals = new HashSet<>();
    this.sharedGoals = new ArrayList<>();

    // Count the rows used for goals
    for (Goal<?> goal : gmm.getGoals()) {
      if (goal.hasShowOption(ShowOption.SHOW_SIDEBAR)
          && goal.getScoreboardFilter().response(match)) {
        if (goal.isShared()) {
          sharedGoals.add(goal);
        } else {
          competitorsWithGoals.addAll(gmm.getCompetitors(goal));
        }
      }
    }
    this.isSuperCompact = isSuperCompact();
  }

  public void startSection() {
    addSpace = rows.size() > 0;
  }

  public void addRow(Component row) {
    if (addSpace) {
      this.rows.add(empty());
      addSpace = false;
    }
    this.rows.add(row);
  }

  public List<Component> getResult() {
    // Needs at least one empty row for scoreboard to show
    if (rows.isEmpty()) {
      rows.add(empty());
    }
    return rows;
  }

  public int size() {
    return rows.size();
  }

  public boolean isFull() {
    return rows.size() >= SidebarRenderer.MAX_ROWS;
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  // Determines if wool objectives should be given their own rows, or all shown on 1 row.
  private boolean isCompactWool() {
    WoolMatchModule wmm = match.getModule(WoolMatchModule.class);
    return wmm != null
        && !(wmm.getWools().keySet().size() * 2 - 1 + wmm.getWools().values().size()
            < SidebarRenderer.MAX_ROWS);
  }

  // Determines if all the map objectives can fit onto the scoreboard with empty rows in between.
  private boolean isSuperCompact() {
    int rowsUsed = competitorsWithGoals.size() * 2 - 1;

    if (isCompactWool()) {
      WoolMatchModule wmm = match.needModule(WoolMatchModule.class);
      rowsUsed += wmm.getWools().keySet().size();
    } else {
      GoalMatchModule gmm = match.needModule(GoalMatchModule.class);
      rowsUsed += gmm.getGoals().size();
    }

    return !(rowsUsed < SidebarRenderer.MAX_ROWS);
  }
}
