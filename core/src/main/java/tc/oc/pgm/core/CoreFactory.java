package tc.oc.pgm.core;

import com.google.common.collect.ImmutableSet;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "core")
public class CoreFactory extends ProximityGoalDefinition {
  protected final Region region;
  protected final MaterialData material;
  protected final int leakLevel;
  protected final ImmutableSet<Mode> modeList;
  protected final boolean showProgress;

  public CoreFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      TeamFactory owner,
      @Nullable ProximityMetric proximityMetric,
      Region region,
      MaterialData material,
      int leakLevel,
      @Nullable ImmutableSet<Mode> modeList,
      boolean showProgress) {

    super(id, name, required, showOptions, owner, proximityMetric);
    this.region = region;
    this.material = material;
    this.leakLevel = leakLevel;
    this.modeList = modeList;
    this.showProgress = showProgress;
  }

  public Region getRegion() {
    return this.region;
  }

  public ImmutableSet<Mode> getModes() {
    return this.modeList;
  }

  public MaterialData getMaterial() {
    return this.material;
  }

  public int getLeakLevel() {
    return this.leakLevel;
  }

  public boolean getShowProgress() {
    return this.showProgress;
  }
}
