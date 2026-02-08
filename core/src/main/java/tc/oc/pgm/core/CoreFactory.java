package tc.oc.pgm.core;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.material.MaterialMatcher;

@FeatureInfo(name = "core")
public class CoreFactory extends ProximityGoalDefinition {
  @Getter
  protected final Region region;

  @Getter
  protected final MaterialMatcher material;

  @Getter
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
      MaterialMatcher material,
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

  public ImmutableSet<Mode> getModes() {
    return this.modeList;
  }

  public boolean getShowProgress() {
    return this.showProgress;
  }
}
