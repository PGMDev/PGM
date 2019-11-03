package tc.oc.pgm.goals;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;
import java.util.Map.Entry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorAddEvent;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.match.MatchModule;

@ListenerScope(MatchScope.LOADED)
public class GoalMatchModule extends MatchModule implements Listener {

  protected static final Sound GOOD_SOUND = new Sound("portal.travel", 0.7f, 2f);
  protected static final Sound BAD_SOUND = new Sound("mob.blaze.death", 0.8f, 0.8f);

  protected final List<Goal> goals = new ArrayList<>();
  protected final Multimap<Competitor, Goal> goalsByCompetitor = ArrayListMultimap.create();
  protected final Multimap<Goal, Competitor> competitorsByGoal = HashMultimap.create();
  protected final Map<Competitor, GoalProgress> progressByCompetitor = new HashMap<>();

  public GoalMatchModule(Match match) {
    super(match);
  }

  public Collection<Goal> getGoals() {
    return goals;
  }

  public Collection<Goal> getGoals(Competitor competitor) {
    return goalsByCompetitor.get(competitor);
  }

  public Collection<Competitor> getCompetitors(Goal goal) {
    return competitorsByGoal.get(goal);
  }

  public Multimap<Competitor, Goal> getGoalsByCompetitor() {
    return goalsByCompetitor;
  }

  public Multimap<Goal, Competitor> getCompetitorsByGoal() {
    return competitorsByGoal;
  }

  public void addGoal(Goal<?> goal) {
    logger.fine("Adding goal " + goal);

    if (!goal.isVisible()) return;

    if (goals.isEmpty()) {
      logger.fine("First goal added, appending " + GoalsVictoryCondition.class.getSimpleName());
      getMatch().addVictoryCondition(new GoalsVictoryCondition());
    }

    goals.add(goal);

    for (Competitor competitor : match.getCompetitors()) {
      addCompetitorGoal(competitor, goal);
    }
  }

  private void addCompetitorGoal(Competitor competitor, Goal<?> goal) {
    if (goal.canComplete(competitor)) {
      logger.fine("Competitor " + competitor + " can complete goal " + goal);

      goalsByCompetitor.put(competitor, goal);
      competitorsByGoal.put(goal, competitor);
    }
  }

  @EventHandler
  public void onCompetitorAdd(CompetitorAddEvent event) {
    logger.fine("Competitor added " + event.getCompetitor());

    for (Goal goal : goals) {
      addCompetitorGoal(event.getCompetitor(), goal);
    }
  }

  @EventHandler
  public void onCompetitorRemove(CompetitorRemoveEvent event) {
    goalsByCompetitor.removeAll(event.getCompetitor());
    for (Goal goal : competitorsByGoal.keySet()) {
      competitorsByGoal.remove(goal, event.getCompetitor());
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends Goal> Multimap<Competitor, T> getGoals(Class<T> filterClass) {
    Multimap<Competitor, T> filteredGoals = ArrayListMultimap.create();
    for (Entry<Competitor, Goal> entry : this.goalsByCompetitor.entries()) {
      if (filterClass.isInstance(entry.getValue())) {
        filteredGoals.put(entry.getKey(), (T) entry.getValue());
      }
    }
    return filteredGoals;
  }

  public GoalProgress getProgress(Competitor competitor) {
    GoalProgress progress = progressByCompetitor.get(competitor);
    if (progress == null) {
      progress = new GoalProgress(competitor);
      progressByCompetitor.put(competitor, progress);
    }
    return progress;
  }

  protected void updateProgress(Goal goal) {
    for (Competitor competitor : competitorsByGoal.get(goal)) {
      progressByCompetitor.put(competitor, new GoalProgress(competitor));
    }
    getMatch().calculateVictory();
  }

  // TODO: These events will often be fired together.. debounce them somehow?

  @EventHandler
  public void onComplete(GoalCompleteEvent event) {
    updateProgress(event.getGoal());

    // Don't play the objective sound if the match is over, because the win/lose sound will play
    // instead
    if (!getMatch().calculateVictory() && event.getGoal().isVisible()) {
      for (MatchPlayer player : event.getMatch().getPlayers()) {
        if (player.getParty() instanceof Competitor
            && event.isGood() != (event.getCompetitor() == player.getParty())) {
          player.playSound(BAD_SOUND);
        } else {
          player.playSound(GOOD_SOUND);
        }
      }
    }
  }

  @EventHandler
  public void onStatusChange(GoalStatusChangeEvent event) {
    updateProgress(event.getGoal());
  }

  @EventHandler
  public void onProximityChange(GoalProximityChangeEvent event) {
    updateProgress(event.getGoal());
  }

  @EventHandler
  public void onTouch(GoalTouchEvent event) {
    updateProgress(event.getGoal());
  }
}
