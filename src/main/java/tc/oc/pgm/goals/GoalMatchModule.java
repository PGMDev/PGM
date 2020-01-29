package tc.oc.pgm.goals;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorAddEvent;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.teams.TeamMatchModule;

@ListenerScope(MatchScope.LOADED)
public class GoalMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<GoalMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getWeakDependencies() {
      return ImmutableList.of(TeamMatchModule.class, FreeForAllMatchModule.class);
    }

    @Override
    public GoalMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new GoalMatchModule(match);
    }
  }

  protected static final Sound GOOD_SOUND = new Sound("portal.travel", 0.7f, 2f);
  protected static final Sound BAD_SOUND = new Sound("mob.blaze.death", 0.8f, 0.8f);

  protected final Match match;
  protected final List<Goal> goals = new ArrayList<>();
  protected final Multimap<Competitor, Goal> goalsByCompetitor = ArrayListMultimap.create();
  protected final Multimap<Goal, Competitor> competitorsByGoal = HashMultimap.create();
  protected final Map<Competitor, GoalProgress> progressByCompetitor = new HashMap<>();

  private GoalMatchModule(Match match) {
    this.match = match;
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
    match.getLogger().fine("Adding goal " + goal);

    if (!goal.isVisible()) return;

    if (goals.isEmpty()) {
      match
          .getLogger()
          .fine("First goal added, appending " + GoalsVictoryCondition.class.getSimpleName());
      match.addVictoryCondition(new GoalsVictoryCondition());
    }

    goals.add(goal);

    for (Competitor competitor : match.getCompetitors()) {
      addCompetitorGoal(competitor, goal);
    }
  }

  private void addCompetitorGoal(Competitor competitor, Goal<?> goal) {
    if (goal.canComplete(competitor)) {
      match.getLogger().fine("Competitor " + competitor + " can complete goal " + goal);

      goalsByCompetitor.put(competitor, goal);
      competitorsByGoal.put(goal, competitor);
    }
  }

  @EventHandler
  public void onCompetitorAdd(CompetitorAddEvent event) {
    match.getLogger().fine("Competitor added " + event.getCompetitor());

    for (Goal goal : goals) {
      addCompetitorGoal(event.getCompetitor(), goal);
    }
  }

  @EventHandler
  public void onCompetitorRemove(CompetitorRemoveEvent event) {
    goalsByCompetitor.removeAll(event.getCompetitor());
    for (Goal goal : ImmutableSet.copyOf(competitorsByGoal.keySet())) {
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
    match.calculateVictory();
  }

  // TODO: These events will often be fired together.. debounce them somehow?

  @EventHandler
  public void onComplete(GoalCompleteEvent event) {
    updateProgress(event.getGoal());

    // Don't play the objective sound if the match is over, because the win/lose sound will play
    // instead
    if (!match.calculateVictory() && event.getGoal().isVisible()) {
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
