package tc.oc.pgm.controlpoint;

import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ControllableGoalDefinition;
import tc.oc.pgm.teams.TeamFactory;

/**
 * An objective that is completed by players being inside a capture region for some amount of time.
 */
@FeatureInfo(name = "control-point")
public class ControlPointDefinition extends ControllableGoalDefinition {
  // Players in this region are considered "on" the point
  private final Region captureRegion;

  // Blocks in this region are used to show capturing progress
  private final Region progressDisplayRegion;

  // Blocks in this region are used to show the team that owns the point
  private final Region ownerDisplayRegion;

  // Block types used for the regions above (currently fixed to wool and stained clay)
  private final Filter visualMaterials;

  // Location of a beacon used to indicate to players that they can capture this point
  private final BlockVector capturableDisplayBeacon;

  // Base time for the point to transition between states
  private final Duration timeToCapture;

  // Capture time multiplier for increasing or decreasing capture time based on the number of
  // players on the point
  private final float timeMultiplier;

  // The team that owns the point when the match starts, null for no owner (neutral state) or ffa
  @Nullable private final TeamFactory initialOwner;

  // true: progress is retained if capturing is interrupted
  // false: progress resets to zero if capturing is interrupted
  private final boolean incrementalCapture;

  // true: point must transition through unowned state to change owners
  // false: point transitions directly from one owner to the next
  // NOTE: points always start in an unowned state, regardless of this value
  private final boolean neutralState;

  // Rate that the owner's score increases, or 0 if the CP does not affect score
  private final float pointsPerSecond;

  // Set number of points given to owner
  private final float pointsOwner;

  // If this is less than +inf, the effective pointsPerSecond will increase over time
  // at an exponential rate, such that it doubles every time this many seconds elapses.
  private final float pointsGrowth;

  public ControlPointDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      Region captureRegion,
      Filter controlFilter,
      Filter dominateFilter,
      Region progressDisplayRegion,
      Region ownerDisplayRegion,
      Filter visualMaterials,
      BlockVector capturableDisplayBeacon,
      Duration timeToCapture,
      float timeMultiplier,
      @Nullable TeamFactory initialOwner,
      CaptureCondition captureCondition,
      boolean incrementalCapture,
      boolean neutralState,
      boolean permanent,
      float pointsPerSecond,
      float pointsOwner,
      float pointsGrowth,
      boolean progress) {

    super(
        id,
        name,
        required,
        visible,
        controlFilter,
        dominateFilter,
        captureCondition,
        permanent,
        progress);
    this.captureRegion = captureRegion;
    this.progressDisplayRegion = progressDisplayRegion;
    this.ownerDisplayRegion = ownerDisplayRegion;
    this.visualMaterials = visualMaterials;
    this.capturableDisplayBeacon = capturableDisplayBeacon;
    this.timeToCapture = timeToCapture;
    this.timeMultiplier = timeMultiplier;
    this.initialOwner = initialOwner;
    this.incrementalCapture = incrementalCapture;
    this.neutralState = neutralState;
    this.pointsPerSecond = pointsPerSecond;
    this.pointsOwner = pointsOwner;
    this.pointsGrowth = pointsGrowth;
  }

  @Override
  public String toString() {
    return "ControlPointDefinition {name="
        + this.getName()
        + " id="
        + this.getId()
        + " timeToCapture="
        + this.getTimeToCapture()
        + " timeMultiplier="
        + this.getTimeMultiplier()
        + " initialOwner="
        + this.getInitialOwner()
        + " captureCondition="
        + this.getCaptureCondition()
        + " incrementalCapture="
        + this.isIncrementalCapture()
        + " neutralState="
        + this.hasNeutralState()
        + " permanent="
        + this.isPermanent()
        + " captureRegion="
        + this.getCaptureRegion()
        + " controlFilter="
        + this.getControlFilter()
        + " dominateFilter="
        + this.getDominateFilter()
        + " progressDisplay="
        + this.getProgressDisplayRegion()
        + " ownerDisplay="
        + this.getControllerDisplayRegion()
        + " beacon="
        + this.getCapturableDisplayBeacon()
        + " visible="
        + this.isVisible();
  }

  public Region getCaptureRegion() {
    return this.captureRegion;
  }

  public Region getProgressDisplayRegion() {
    return this.progressDisplayRegion;
  }

  public Region getControllerDisplayRegion() {
    return this.ownerDisplayRegion;
  }

  public Filter getVisualMaterials() {
    return this.visualMaterials;
  }

  public BlockVector getCapturableDisplayBeacon() {
    return this.capturableDisplayBeacon;
  }

  public Duration getTimeToCapture() {
    return this.timeToCapture;
  }

  public float getTimeMultiplier() {
    return this.timeMultiplier;
  }

  @Nullable
  public TeamFactory getInitialOwner() {
    return this.initialOwner;
  }

  public boolean isIncrementalCapture() {
    return this.incrementalCapture;
  }

  public boolean hasNeutralState() {
    return this.neutralState;
  }

  public boolean affectsScore() {
    return this.pointsPerSecond > 0;
  }

  public float getPointsPerSecond() {
    return this.pointsPerSecond;
  }

  public float getPointsOwner() {
    return this.pointsOwner;
  }

  public float getPointsGrowth() {
    return this.pointsGrowth;
  }
}
