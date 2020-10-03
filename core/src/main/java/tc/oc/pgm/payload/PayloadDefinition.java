package tc.oc.pgm.payload;

import java.util.List;
import java.util.Map;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.goals.ControllableGoalDefinition;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "payload")
public class PayloadDefinition extends ControllableGoalDefinition {

  /** The relative "middle" of the payload path(used for checkpoints and neutral movement) */
  private final Vector middleLocation;

  private final Map<TeamFactory, Vector> goalsByTeam;
  private final Map<TeamFactory, Float> speedsByTeam;

  /** The speed of the payload when it is in a neutral state */
  private final float neutralSpeed;

  /** The radius of the payload (detecting players) */
  private final float radius;

  /** The height of the payload (detecting players) */
  private final float height;

  private final List<PayloadCheckpoint> checkpoints;

  /** Amount of points given to the team that captures the payload */
  private final float points;

  PayloadDefinition(
      String id,
      String name,
      Boolean required,
      boolean visible,
      Vector middleLocation,
      Filter controlFilter,
      Filter dominateFilter,
      Map<TeamFactory, Vector> goalsByTeam,
      Map<TeamFactory, Float> speedsByTeam,
      CaptureCondition captureCondition,
      float radius,
      float height,
      List<PayloadCheckpoint> checkpoints,
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
    this.middleLocation = middleLocation;
    this.goalsByTeam = goalsByTeam;
    this.speedsByTeam = speedsByTeam;
    this.radius = radius;
    this.height = height;
    this.checkpoints = checkpoints;
    this.neutralSpeed = neutralSpeed;
    this.points = points;
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

  public List<PayloadCheckpoint> getCheckpoints() {
    return checkpoints;
  }

  public Float getSpeed(Team team) {
    return speedsByTeam.get(team.getInfo());
  }

  public float getNeutralSpeed() {
    return neutralSpeed;
  }

  public float getPoints() {
    return this.points;
  }

  public Vector getGoal(Team team) {
    return goalsByTeam.get(team.getInfo());
  }

  @Override
  public String toString() {
    return "PayloadDefinition{"
        + "middleLocation="
        + middleLocation
        + ", goalsByTeam="
        + goalsByTeam
        + ", speedsByTeam="
        + speedsByTeam
        + ", neutralSpeed="
        + neutralSpeed
        + ", radius="
        + radius
        + ", height="
        + height
        + ", checkpoints="
        + checkpoints
        + ", points="
        + points
        + '}';
  }
}
