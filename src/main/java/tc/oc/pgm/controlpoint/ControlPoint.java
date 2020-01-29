package tc.oc.pgm.controlpoint;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.controlpoint.events.CapturingTeamChangeEvent;
import tc.oc.pgm.controlpoint.events.CapturingTimeChangeEvent;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.SimpleGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.util.StringUtils;
import tc.oc.util.collection.DefaultMapAdapter;

public class ControlPoint extends SimpleGoal<ControlPointDefinition>
    implements IncrementalGoal<ControlPointDefinition> {

  public static final ChatColor COLOR_NEUTRAL_TEAM = ChatColor.WHITE;

  public static final String SYMBOL_CP_INCOMPLETE = "\u29be"; // ⦾
  public static final String SYMBOL_CP_COMPLETE = "\u29bf"; // ⦿

  protected final ControlPointPlayerTracker playerTracker;
  protected final ControlPointBlockDisplay blockDisplay;

  protected final Vector centerPoint;

  // This is set false after the first state change if definition.permanent == true
  protected boolean capturable = true;

  // The team that currently owns the point. The goal is completed for this team.
  // If this is null then the point is unowned, either because it is in the
  // neutral state, or because it has no initial owner and has not yet been captured.
  protected Team controllingTeam = null;

  // The team that will own the CP if the current capture is successful.
  // If this is null then either the point is not being captured or it is
  // being "uncaptured" toward the neutral state.
  protected Team capturingTeam = null;

  // Time accumulated towards the owner change. When this passes timeToCaptureMillis,
  // it is reset to zero and the capturingTeam becomes the controllingTeam. When this is zero,
  // the capturingTeam is null.
  protected Duration capturingTime = Duration.ZERO;

  public ControlPoint(Match match, ControlPointDefinition definition) {
    super(definition, match);

    if (this.definition.getInitialOwner() != null) {
      this.controllingTeam =
          match.needModule(TeamMatchModule.class).getTeam(this.definition.getInitialOwner());
    }

    this.centerPoint = this.getCaptureRegion().getBounds().getCenterPoint();

    this.playerTracker = new ControlPointPlayerTracker(match, this.getCaptureRegion());

    this.blockDisplay = new ControlPointBlockDisplay(match, this);
  }

  public void registerEvents() {
    this.match.addListener(this.playerTracker, MatchScope.RUNNING);
    this.match.addListener(this.blockDisplay, MatchScope.RUNNING);

    this.blockDisplay.render();
  }

  public void unregisterEvents() {
    HandlerList.unregisterAll(this.blockDisplay);
    HandlerList.unregisterAll(this.playerTracker);
  }

  public ControlPointBlockDisplay getBlockDisplay() {
    return blockDisplay;
  }

  public ControlPointPlayerTracker getPlayerTracker() {
    return playerTracker;
  }

  public Region getCaptureRegion() {
    return definition.getCaptureRegion();
  }

  public Duration getTimeToCapture() {
    return definition.getTimeToCapture();
  }

  /** Point that can be used as the location of the ControlPoint */
  public Vector getCenterPoint() {
    return centerPoint.clone();
  }

  /**
   * The team that owns (is receiving points from) this ControlPoint, or null if the ControlPoint is
   * unowned.
   */
  public Team getControllingTeam() {
    return this.controllingTeam;
  }

  /**
   * The team that is "capturing" the ControlPoint. This is the team that the current capturingTime
   * counts towards. The capturingTime goes up whenever this team has the most players on the point,
   * and goes down when any other team has the most players on the point. If capturingTime reaches
   * timeToCapture, this team will take ownership of the point, if they don't own it already. When
   * capturingTime goes below zero, the capturingTeam changes to the team with the most players on
   * the point, and the point becomes unowned.
   */
  public Team getCapturingTeam() {
    return this.capturingTeam;
  }

  /**
   * The partial owner of the ControlPoint. The "partial owner" is defined in three scenarios. If
   * the ControlPoint is owned and has a neutral state, the partial owner is the owner of the
   * ControlPoint. If the ControlPoint is in contest, the partial owner is the team that is
   * currently capturing the ControlPoint. Lastly, if the ControlPoint is un-owned and not in
   * contest, the progressingTeam is null.
   *
   * @return The team that should be displayed as having partial ownership of the point, if any.
   */
  public Team getPartialOwner() {
    if (this.definition.hasNeutralState() && this.getControllingTeam() != null) {
      return this.getControllingTeam();
    } else {
      return this.getCapturingTeam();
    }
  }

  /** Progress towards "capturing" the ControlPoint for the current capturingTeam */
  public Duration getCapturingTime() {
    return this.capturingTime;
  }

  /**
   * Progress toward "capturing" the ControlPoint for the current capturingTeam, as a real number
   * from 0 to 1.
   */
  @Override
  public double getCompletion() {
    return (double) this.capturingTime.getMillis()
        / (double) this.definition.getTimeToCapture().getMillis();
  }

  @Override
  public String renderCompletion() {
    return StringUtils.percentage(this.getCompletion());
  }

  @Override
  public @Nullable String renderPreciseCompletion() {
    return null;
  }

  @Override
  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return this.capturingTeam == null ? COLOR_NEUTRAL_TEAM : this.capturingTeam.getColor();
  }

  @Override
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (Duration.ZERO.equals(this.capturingTime)) {
      return this.controllingTeam == null ? SYMBOL_CP_INCOMPLETE : SYMBOL_CP_COMPLETE;
    } else {
      return this.renderCompletion();
    }
  }

  @Override
  public ChatColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
    return this.controllingTeam == null ? COLOR_NEUTRAL_TEAM : this.controllingTeam.getColor();
  }

  /** Ownership of the ControlPoint for a specific team given as a real number from 0 to 1. */
  public double getCompletion(Team team) {
    if (this.getControllingTeam() == team) {
      return 1 - this.getCompletion();
    } else if (this.getCapturingTeam() == team) {
      return this.getCompletion();
    } else {
      return 0;
    }
  }

  @Override
  public boolean getShowProgress() {
    return this.definition.getShowProgress();
  }

  @Override
  public boolean isShared() {
    return true;
  }

  @Override
  public boolean canComplete(Competitor team) {
    return this.canCapture(team);
  }

  @Override
  public boolean isCompleted() {
    return this.controllingTeam != null;
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return this.controllingTeam != null && this.controllingTeam == team;
  }

  private boolean canCapture(Competitor team) {
    return this.definition.getCaptureFilter() == null
        || this.definition.getCaptureFilter().query(team.getQuery()).isAllowed();
  }

  private boolean canDominate(MatchPlayer player) {
    return this.definition.getPlayerFilter() == null
        || this.definition.getPlayerFilter().query(player.getQuery()).isAllowed();
  }

  public float getEffectivePointsPerSecond() {
    float seconds = this.getMatch().getDuration().getStandardSeconds();
    float initial = this.getDefinition().getPointsPerSecond();
    float growth = this.getDefinition().getPointsGrowth();
    return (float) (initial * Math.pow(2, seconds / growth));
  }

  private Duration calculateDominateTime(int lead, Duration duration) {
    // Don't scale time if only one player is present, don't zero duration if multiplier is zero
    float msTime =
        duration.getMillis() * (1 + (lead - 1) * this.getDefinition().getTimeMultiplier());

    return Duration.millis(Math.round(msTime));
  }

  public void tick(Duration duration) {
    this.tickCapture(duration);
    this.tickScore(duration);
  }

  /** Do a scoring cycle on this ControlPoint over the given duration. */
  protected void tickScore(Duration duration) {
    if (this.getControllingTeam() != null && this.getDefinition().affectsScore()) {
      ScoreMatchModule scoreMatchModule = this.getMatch().getModule(ScoreMatchModule.class);
      if (scoreMatchModule != null) {
        float seconds = this.getMatch().getDuration().getStandardSeconds();
        float initial = this.getDefinition().getPointsPerSecond();
        float growth = this.getDefinition().getPointsGrowth();
        float rate = (float) (initial * Math.pow(2, seconds / growth));
        scoreMatchModule.incrementScore(
            this.getControllingTeam(), rate * duration.getMillis() / 1000d);
      }
    }
  }

  /** Do a capturing cycle on this ControlPoint over the given duration. */
  protected void tickCapture(Duration duration) {
    Map<Team, Integer> playerCounts = new DefaultMapAdapter<>(new HashMap<Team, Integer>(), 0);

    // The teams with the most and second-most capturing players on the point, respectively
    Team leader = null, runnerUp = null;

    // The total number of players on the point who are allowed to dominate and not on the leading
    // team
    int defenderCount = 0;

    for (MatchPlayer player : this.playerTracker.getPlayersOnPoint()) {
      Team team = Teams.get(player);
      if (this.canDominate(player)) {
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

      switch (this.definition.getCaptureCondition()) {
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
      this.dominateAndFireEvents(leader, calculateDominateTime(lead, duration));
    } else {
      this.dominateAndFireEvents(null, duration);
    }
  }

  /**
   * Do a cycle of domination on this ControlPoint for the given team over the given duration. The
   * team can be null, which means no team is dominating the point, which can cause the state to
   * change in some configurations.
   */
  private void dominateAndFireEvents(@Nullable Team dominantTeam, Duration dominantTime) {
    Duration oldCapturingTime = this.capturingTime;
    Team oldCapturingTeam = this.capturingTeam;
    Team oldControllingTeam = this.controllingTeam;

    this.dominate(dominantTeam, dominantTime);

    if (oldCapturingTeam != this.capturingTeam || !oldCapturingTime.equals(this.capturingTime)) {
      this.match.callEvent(new CapturingTimeChangeEvent(this.match, this));
      this.match.callEvent(new GoalStatusChangeEvent(this.match, this, null));
    }

    if (oldCapturingTeam != this.capturingTeam) {
      this.match.callEvent(
          new CapturingTeamChangeEvent(this.match, this, oldCapturingTeam, this.capturingTeam));
    }

    if (oldControllingTeam != this.controllingTeam) {
      this.match.callEvent(
          new ControllerChangeEvent(this.match, this, oldControllingTeam, this.controllingTeam));

      if (this.controllingTeam == null) {
        this.match.callEvent(new GoalCompleteEvent(this.match, this, oldControllingTeam, false));
      } else {
        this.match.callEvent(new GoalCompleteEvent(this.match, this, this.controllingTeam, true));
      }
    }
  }

  /**
   * If there is a neutral state, then the point cannot be owned and captured at the same time. This
   * means that at least one of controllingTeam or capturingTeam must be null at any particular
   * time.
   *
   * <p>If controllingTeam is non-null, the point is owned, and it must be "uncaptured" before any
   * other team can capture it. In this state, capturingTeam is null, the controlling team will
   * decrease capturingTimeMillis, and all other teams will increase it.
   *
   * <p>If controllingTeam is null, then the point is in the neutral state. If capturingTeam is also
   * null, then the point is not being captured, and capturingTimeMillis is zero. If capturingTeam
   * is non-null, then that is the only team that will increase capturingTimeMillis. All other teams
   * will decrease it.
   *
   * <p>If there is no neutral state, then the point is always either being captured by a specific
   * team, or not being captured at all.
   *
   * <p>If incremental capturing is disabled, then capturingTimeMillis is reset to zero whenever it
   * stops increasing.
   */
  private void dominate(Team dominantTeam, Duration dominantTime) {
    if (!this.capturable || !dominantTime.isLongerThan(Duration.ZERO)) {
      return;
    }

    ControlPointDefinition definition = this.getDefinition();

    if (this.controllingTeam != null && definition.hasNeutralState()) {
      // Point is owned and must go through the neutral state before another team can capture it
      if (dominantTeam == this.controllingTeam) {
        this.regressCapture(dominantTeam, dominantTime);
      } else if (dominantTeam != null) {
        this.progressUncapture(dominantTeam, dominantTime);
      } else if (!definition.isIncrementalCapture()) {
        // No team is dominant and point is not incremental, so reset the time
        this.capturingTime = Duration.ZERO;
      }
    } else if (this.capturingTeam != null) {
      // Point is being captured by a specific team
      if (dominantTeam == this.capturingTeam) {
        this.progressCapture(dominantTeam, dominantTime);
      } else if (dominantTeam != null) {
        this.regressCapture(dominantTeam, dominantTime);
      } else if (!definition.isIncrementalCapture()) {
        // No team is dominant and point is not incremental, so reset time and clear capturing team
        this.capturingTime = Duration.ZERO;
        this.capturingTeam = null;
      }
    } else if (dominantTeam != null
        && dominantTeam != this.controllingTeam
        && this.canCapture(dominantTeam)) {
      // Point is not being captured and there is a dominant team that is not the owner, so they
      // start capturing
      this.capturingTeam = dominantTeam;
      this.dominate(dominantTeam, dominantTime);
    }
  }

  /** Progress toward the neutral state */
  private void progressUncapture(Team dominantTeam, Duration dominantTime) {
    this.capturingTime = this.capturingTime.plus(dominantTime);

    if (!this.capturingTime.isShorterThan(this.definition.getTimeToCapture())) {
      // If uncapture is complete, recurse with the dominant team's remaining time
      dominantTime = this.capturingTime.minus(this.definition.getTimeToCapture());
      this.capturingTime = Duration.ZERO;
      this.controllingTeam = null;
      this.dominate(dominantTeam, dominantTime);
    }
  }

  /** Progress toward a new controller */
  private void progressCapture(Team dominantTeam, Duration dominantTime) {
    this.capturingTime = this.capturingTime.plus(dominantTime);
    if (!this.capturingTime.isShorterThan(this.definition.getTimeToCapture())) {
      this.capturingTime = Duration.ZERO;
      this.controllingTeam = this.capturingTeam;
      this.capturingTeam = null;
      if (this.getDefinition().isPermanent()) {
        // The objective is permanent, so the first capture disables it
        this.capturable = false;
      }
    }
  }

  /** Regress toward the current state */
  private void regressCapture(Team dominantTeam, Duration dominantTime) {
    boolean crossZero = false;
    if (definition.isIncrementalCapture()) {
      // For incremental points, decrease the capture time
      if (this.capturingTime.isLongerThan(dominantTime)) {
        this.capturingTime = this.capturingTime.minus(dominantTime);
      } else {
        dominantTime = dominantTime.minus(this.capturingTime);
        this.capturingTime = Duration.ZERO;
        crossZero = true;
      }
    } else {
      // For non-incremental points, reset capture time to zero
      this.capturingTime = Duration.ZERO;
      crossZero = true;
    }

    if (crossZero) {
      this.capturingTeam = null;
      if (dominantTeam != this.controllingTeam) {
        // If the dominant team is not the controller, recurse with the remaining time
        this.dominate(dominantTeam, dominantTime);
      }
    }
  }
}
