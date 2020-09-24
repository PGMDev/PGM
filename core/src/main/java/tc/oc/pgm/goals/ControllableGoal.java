package tc.oc.pgm.goals;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.regions.RegionPlayerTracker;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.collection.DefaultMapAdapter;

public abstract class ControllableGoal<T extends ControllableGoalDefinition> extends SimpleGoal<T>
    implements Tickable {

  protected final RegionPlayerTracker playerTracker;

  protected Region controllableRegion;

  public ControllableGoal(T definition, Match match) {
    super(definition, match);
    playerTracker = new RegionPlayerTracker(match, controllableRegion);
    match.addTickable(this, MatchScope.RUNNING);
  }

  private final Duration tick = Duration.ofMillis(TimeUtils.TICK);

  @Override
  public void tick(Match match, Tick tick) {
    contestCycle(this.tick);
  }

  /** Do a cycle to check which team(if any) is dominating the goal */
  public void contestCycle(Duration duration) {
    Map<Competitor, Integer> playerCounts = new DefaultMapAdapter<>(new HashMap<>(), 0);

    // The teams with the most and second-most capturing players on the payload, respectively
    Competitor leader = null, runnerUp = null;

    // The total number of players on the point who are allowed to dominate and not on the leading
    // team
    int defenderCount = 0;

    for (MatchPlayer player : playerTracker.getPlayersInRegion()) {
      Competitor team = player.getCompetitor();
      if (canContest(player)) {
        defenderCount++;
        int playerCount = playerCounts.get(team) + 1;
        playerCounts.put(team, playerCount);

        if (team != leader) {
          if (leader == null || playerCount > playerCounts.get(leader)) {
            runnerUp = leader;
            leader = team;
          } else if (team != runnerUp
              && (runnerUp == null || playerCount > playerCounts.get(runnerUp))) {
            runnerUp = team;
          }
        }
      }
    }

    int lead = 0;
    if (leader != null) {
      lead = playerCounts.get(leader);
      defenderCount -= lead;

      switch (definition.getCaptureCondition()) {
        case EXCLUSIVE:
          if (defenderCount > 0) {
            lead = 0;
          }
          break;

        case MAJORITY:
          lead = Math.max(0, lead - defenderCount);
          break;

        case LEAD:
          if (runnerUp != null) {
            lead -= playerCounts.get(runnerUp);
          }
          break;
      }
    }

    if (lead > 0) {
      dominationCycle(leader, lead, duration);
    } else {
      dominationCycle(null, lead, duration);
    }
  }

  /** Do a cycle on domination to determine who should control the goal */
  public abstract void dominationCycle(
      @Nullable Competitor dominatingTeam, int lead, Duration duration);

  protected boolean canContest(MatchPlayer player) {
    return definition.getDominateFilter() == null
        || definition.getDominateFilter().query(player.getQuery()).isAllowed();
  }

  protected boolean canDominate(Competitor team) {
    return definition.getControlFilter() == null
        || definition.getControlFilter().query(team.getQuery()).isAllowed();
  }

  // The region can change during the match
  protected void setControllableRegion(Region region) {
    this.controllableRegion = region;
    playerTracker.setRegion(region);
  }

  public Region getControllableRegion() {
    return controllableRegion;
  }

  public Set<MatchPlayer> getPlayersOnGoal() {
    return playerTracker.getPlayersInRegion();
  }
}
