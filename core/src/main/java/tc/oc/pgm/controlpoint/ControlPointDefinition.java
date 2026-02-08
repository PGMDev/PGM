package tc.oc.pgm.controlpoint;

import java.time.Duration;
import lombok.Getter;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.teams.TeamFactory;

/**
 * An objective that is completed by players being inside a capture region for some amount of time.
 */
@FeatureInfo(name = "control-point")
public class ControlPointDefinition extends GoalDefinition {
  // Players in this region are considered "on" the point
  @Getter
  private final Region captureRegion;

  // Which players can capture the point
  @Getter
  private final Filter captureFilter;

  // Which players can prevent other teams from capturing the point
  @Getter
  private final Filter playerFilter;

  // Blocks in this region are used to show capturing progress
  @Getter
  private final Region progressDisplayRegion;

  // Blocks in this region are used to show the team that owns the point
  @Getter
  private final Region ownerDisplayRegion;

  // Block types used for the regions above (currently fixed to wool and stained clay)
  @Getter
  private final Filter visualMaterials;

  // Location of a beacon used to indicate to players that they can capture this point
  @Getter
  private final BlockVector capturableDisplayBeacon;

  // Base time for the point to transition between states
  @Getter
  private final Duration timeToCapture;

  // Time it takes for a point to decay while unowned. (Time is accurate when near 100% capture)
  @Getter
  private final double decayRate;

  // Time it takes for a point to decay while contested. (Time is accurate when near 100% capture)
  @Getter
  private final double contestedRate;

  // Time it takes for a point to recover to captured state. (Accurate when almost uncaptured)
  @Getter
  private final double recoveryRate;

  // Time it takes for a point to transition to neutral state.
  @Getter
  private final double ownedDecayRate;

  // Capture time multiplier for increasing or decreasing capture time based on the number of
  // players on the point
  @Getter
  private final float timeMultiplier;

  // The team that owns the point when the match starts, null for no owner (neutral state) or ffa
  @Getter
  private final @Nullable TeamFactory initialOwner;

  // Conditions required for a team to capture:
  public enum CaptureCondition {
    EXCLUSIVE, // Team owns all players on the point
    MAJORITY, // Team owns more than half the players on the point
    LEAD // Team owns more players on the point than any other single team
  }

  @Getter
  private final CaptureCondition captureCondition;

  // true: point must transition through unowned state to change owners
  // false: point transitions directly from one owner to the next
  // NOTE: points always start in an unowned state, regardless of this value
  private final boolean neutralState;

  // If true, the point can only be captured once in the match
  @Getter
  private final boolean permanent;

  // Rate that the owner's score increases, or 0 if the CP does not affect score
  @Getter
  private final float pointsPerSecond;

  // Set number of points given to owner
  @Getter
  private final float pointsOwner;

  // If this is less than +inf, the effective pointsPerSecond will increase over time
  // at an exponential rate, such that it doubles every time this many seconds elapses.
  @Getter
  private final float pointsGrowth;

  // If true, capturing progress is displayed on the scoreboard
  private final boolean showProgress;

  public ControlPointDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      Region captureRegion,
      Filter captureFilter,
      Filter playerFilter,
      Region progressDisplayRegion,
      Region ownerDisplayRegion,
      Filter visualMaterials,
      BlockVector capturableDisplayBeacon,
      Duration timeToCapture,
      double decayRate,
      double recoveryRate,
      double ownedDecayRate,
      double contestedRate,
      float timeMultiplier,
      @Nullable TeamFactory initialOwner,
      CaptureCondition captureCondition,
      boolean neutralState,
      boolean permanent,
      float pointsPerSecond,
      float pointsOwner,
      float pointsGrowth,
      boolean progress) {

    super(id, name, required, showOptions);
    this.captureRegion = captureRegion;
    this.captureFilter = captureFilter;
    this.playerFilter = playerFilter;
    this.progressDisplayRegion = progressDisplayRegion;
    this.ownerDisplayRegion = ownerDisplayRegion;
    this.visualMaterials = visualMaterials;
    this.capturableDisplayBeacon = capturableDisplayBeacon;
    this.timeToCapture = timeToCapture;
    this.decayRate = decayRate;
    this.recoveryRate = recoveryRate;
    this.ownedDecayRate = ownedDecayRate;
    this.contestedRate = contestedRate;
    this.timeMultiplier = timeMultiplier;
    this.initialOwner = initialOwner;
    this.captureCondition = captureCondition;
    this.neutralState = neutralState;
    this.permanent = permanent;
    this.pointsPerSecond = pointsPerSecond;
    this.pointsOwner = pointsOwner;
    this.pointsGrowth = pointsGrowth;
    this.showProgress = progress;
  }

  public ControlPoint build(Match match) {
    return new ControlPoint(match, this);
  }

  @Override
  public String toString() {
    return "ControlPointDefinition {name="
        + this.getName()
        + " id="
        + this.getId()
        + " timeToCapture="
        + this.getTimeToCapture()
        + " decayRate="
        + this.getDecayRate()
        + " recoveryRate="
        + this.getRecoveryRate()
        + " ownedDecayRate="
        + this.getOwnedDecayRate()
        + " contestedRate="
        + this.getContestedRate()
        + " timeMultiplier="
        + this.getTimeMultiplier()
        + " initialOwner="
        + this.getInitialOwner()
        + " captureCondition="
        + this.getCaptureCondition()
        + " neutralState="
        + this.hasNeutralState()
        + " permanent="
        + this.isPermanent()
        + " captureRegion="
        + this.getCaptureRegion()
        + " captureFilter="
        + this.getCaptureFilter()
        + " playerFilter="
        + this.getPlayerFilter()
        + " progressDisplay="
        + this.getProgressDisplayRegion()
        + " ownerDisplay="
        + this.getOwnerDisplayRegion()
        + " beacon="
        + this.getCapturableDisplayBeacon()
        + " options="
        + this.getShowOptions();
  }

  @Deprecated
  public Region getControllerDisplayRegion() {
    return getOwnerDisplayRegion();
  }

  public boolean hasNeutralState() {
    return this.neutralState;
  }

  public boolean affectsScore() {
    return this.pointsPerSecond != 0;
  }

  public boolean getShowProgress() {
    return this.showProgress;
  }
}
