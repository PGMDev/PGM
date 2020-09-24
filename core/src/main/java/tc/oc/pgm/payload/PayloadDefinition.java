package tc.oc.pgm.payload;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.goals.ControllableGoalDefinition;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.material.MaterialMatcher;

@FeatureInfo(name = "payload")
public class PayloadDefinition extends ControllableGoalDefinition {

  /** Where the path building starts from(Location of the primary goal) */
  private final Vector startingLocation;

  /** The relative "middle" of the payload path(used for checkpoints and neutral movement) */
  private final Vector middleLocation;

  /** The primary owner, pushed toward the primary goal */
  private final TeamFactory primaryOwner;

  // The optional secondary owner, if present they can push towards the secondary goal
  @Nullable private final TeamFactory secondaryOwner;

  /** The radius of the payload (detecting players) */
  private final float radius;

  /** The height of the payload (detecting players) */
  private final float height;

  /** Determines if the secondary team should be able to push but have no goal */
  private final boolean secondaryTeamPushButNoGoal;

  /** The material(s) of the checkpoint blocks */
  @Nullable private final MaterialMatcher checkpointMaterial;

  @Nullable private final List<Integer> permanentHeadCheckpoints;
  @Nullable private final List<Integer> permanentTailCheckpoints;

  /** The speed of the payload when under control of the owning team */
  private final float primaryOwnerSpeed;

  /** The speed of the payload when under control of the competing owning team */
  private final float secondaryOwnerSpeed;

  /** The speed of the payload when it is in a neutral state */
  private final float neutralSpeed;

  /** Amount of points given to the team that captures the payload */
  private final float points;

  PayloadDefinition(
      String id,
      String name,
      Boolean required,
      boolean visible,
      Vector startingLocation,
      Vector middleLocation,
      Filter controlFilter,
      Filter dominateFilter,
      TeamFactory primaryOwner,
      @Nullable TeamFactory secondaryOwner,
      CaptureCondition captureCondition,
      float radius,
      float height,
      boolean secondaryTeamPushButNoGoal,
      @Nullable MaterialMatcher checkpointMaterial,
      @Nullable List<Integer> permanentHeadCheckpoints,
      @Nullable List<Integer> permanentTailCheckpoints,
      float primaryOwnerSpeed,
      float secondaryOwnerSpeed,
      float neutralSpeed,
      boolean permanent,
      float points,
      boolean showProgress) {
    super(
        id,
        name,
        required,
        visible,
        controlFilter,
        dominateFilter,
        captureCondition,
        permanent,
        showProgress);
    this.startingLocation = startingLocation;
    this.middleLocation = middleLocation;
    this.primaryOwner = primaryOwner;
    this.secondaryOwner = secondaryOwner;
    this.radius = radius;
    this.height = height;
    this.secondaryTeamPushButNoGoal = secondaryTeamPushButNoGoal;
    this.checkpointMaterial = checkpointMaterial;
    this.permanentHeadCheckpoints = permanentHeadCheckpoints;
    this.permanentTailCheckpoints = permanentTailCheckpoints;
    this.primaryOwnerSpeed = primaryOwnerSpeed;
    this.secondaryOwnerSpeed = secondaryOwnerSpeed;
    this.neutralSpeed = neutralSpeed;
    this.points = points;
  }

  public Vector getStartingLocation() {
    return this.startingLocation;
  }

  public Vector getMiddleLocation() {
    return middleLocation;
  }

  public float getRadius() {
    return this.radius;
  }

  public float getHeight() {
    return this.height;
  }

  @Nullable
  public MaterialMatcher getCheckpointMaterial() {
    return checkpointMaterial;
  }

  @Nullable
  public List<Integer> getPermanentHeadCheckpoints() {
    return permanentHeadCheckpoints;
  }

  @Nullable
  public List<Integer> getPermanentTailCheckpoints() {
    return permanentTailCheckpoints;
  }

  public float getPrimaryOwnerSpeed() {
    return this.primaryOwnerSpeed;
  }

  public float getSecondaryOwnerSpeed() {
    return this.secondaryOwnerSpeed;
  }

  public float getPoints() {
    return this.points;
  }

  public TeamFactory getPrimaryOwner() {
    return primaryOwner;
  }

  @Nullable
  public TeamFactory getSecondaryOwner() {
    return secondaryOwner;
  }

  public float getNeutralSpeed() {
    return neutralSpeed;
  }

  /**
   * True if the specified secondary team should have their own push speed without being able to
   * complete the goal
   */
  public boolean shouldSecondaryTeamPushButNoGoal() {
    return secondaryTeamPushButNoGoal;
  }

  @Override
  public String toString() {
    return "PayloadDefinition{"
        + "startingLocation="
        + startingLocation
        + ", primaryOwner="
        + primaryOwner
        + ", secondaryOwner="
        + secondaryOwner
        + ", radius="
        + radius
        + ", height="
        + height
        + ", secondaryTeamPushButNoGoal="
        + secondaryTeamPushButNoGoal
        + ", checkpointMaterial="
        + checkpointMaterial
        + ", permanentHeadCheckpoints="
        + permanentHeadCheckpoints
        + ", permanentTailCheckpoints="
        + permanentTailCheckpoints
        + ", primaryOwnerSpeed="
        + primaryOwnerSpeed
        + ", secondaryOwnerSpeed="
        + secondaryOwnerSpeed
        + ", neutralSpeed="
        + neutralSpeed
        + ", points="
        + points
        + '}';
  }
}
