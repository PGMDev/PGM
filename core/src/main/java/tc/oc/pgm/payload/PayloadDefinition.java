package tc.oc.pgm.payload;

import java.time.Duration;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.ControlPointDefinition;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.teams.TeamFactory;

public class PayloadDefinition extends ControlPointDefinition {

  private final BlockVector location;
  private final double radius;
  private final Filter displayFilter;

  public PayloadDefinition(
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
      boolean progress,
      BlockVector location,
      double radius,
      Filter displayFilter) {
    super(
        id,
        name,
        required,
        showOptions,
        captureRegion,
        captureFilter,
        playerFilter,
        progressDisplayRegion,
        ownerDisplayRegion,
        visualMaterials,
        capturableDisplayBeacon,
        timeToCapture,
        decayRate,
        recoveryRate,
        ownedDecayRate,
        contestedRate,
        timeMultiplier,
        initialOwner,
        captureCondition,
        neutralState,
        permanent,
        pointsPerSecond,
        pointsOwner,
        pointsGrowth,
        progress);
    this.location = location;
    this.radius = radius;
    this.displayFilter = displayFilter;
  }

  public BlockVector getLocation() {
    return location;
  }

  public double getRadius() {
    return radius;
  }

  public Filter getDisplayFilter() {
    return displayFilter;
  }

  public Payload build(Match match) {
    return new Payload(match, this);
  }
}
