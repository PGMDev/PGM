package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

@FeatureInfo(name = "destroyable")
public class DestroyableFactory extends ProximityGoalDefinition {
  protected final Region region;
  protected final Set<SingleMaterialMatcher> materials;
  protected final double destructionRequired;
  protected final ImmutableSet<Mode> modeList;
  protected final boolean showProgress;
  protected final boolean sparks;
  protected final boolean repairable;

  public DestroyableFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      TeamFactory owner,
      ProximityMetric proximityMetric,
      Region region,
      Set<SingleMaterialMatcher> materials,
      double destructionRequired,
      @Nullable ImmutableSet<Mode> modeList,
      boolean showProgress,
      boolean sparks,
      boolean repairable) {
    super(id, name, required, showOptions, owner, proximityMetric);
    this.region = region;
    this.materials = materials;
    this.destructionRequired = destructionRequired;
    this.modeList = modeList;
    this.showProgress = showProgress;
    this.sparks = sparks;
    this.repairable = repairable;
  }

  public Region getRegion() {
    return this.region;
  }

  public Set<SingleMaterialMatcher> getMaterials() {
    return this.materials;
  }

  public ImmutableSet<Mode> getModes() {
    return this.modeList;
  }

  public double getDestructionRequired() {
    return this.destructionRequired;
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
