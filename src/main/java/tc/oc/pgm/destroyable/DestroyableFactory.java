package tc.oc.pgm.destroyable;

import java.util.Set;
import javax.annotation.Nullable;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.features.FeatureInfo;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "destroyable")
public class DestroyableFactory extends ProximityGoalDefinition {
  protected final Region region;
  protected final Set<SingleMaterialMatcher> materials;
  protected final double destructionRequired;
  protected final boolean modeChanges;
  protected final boolean showProgress;
  protected final boolean sparks;
  protected final boolean repairable;
  protected final boolean visible;

  public DestroyableFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      TeamFactory owner,
      ProximityMetric proximityMetric,
      Region region,
      Set<SingleMaterialMatcher> materials,
      double destructionRequired,
      boolean modeChanges,
      boolean showProgress,
      boolean sparks,
      boolean repairable) {
    super(id, name, required, visible, owner, proximityMetric);
    this.region = region;
    this.materials = materials;
    this.destructionRequired = destructionRequired;
    this.modeChanges = modeChanges;
    this.showProgress = showProgress;
    this.sparks = sparks;
    this.repairable = repairable;
    this.visible = visible;
  }

  public Region getRegion() {
    return this.region;
  }

  public Set<SingleMaterialMatcher> getMaterials() {
    return this.materials;
  }

  public double getDestructionRequired() {
    return this.destructionRequired;
  }

  public boolean hasModeChanges() {
    return this.modeChanges;
  }

  public boolean getShowProgress() {
    return this.showProgress;
  }

  public boolean hasSparks() {
    return this.sparks;
  }

  public boolean isRepairable() {
    return this.repairable;
  }
}
