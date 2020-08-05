package tc.oc.pgm.core;

import javax.annotation.Nullable;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "core")
public class CoreFactory extends ProximityGoalDefinition {
  protected final Region region;
  protected final MaterialData material;
  protected final int leakLevel;
  protected final boolean modeChanges;
  protected final boolean showProgress;

  public CoreFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      TeamFactory owner,
      @Nullable ProximityMetric proximityMetric,
      Region region,
      MaterialData material,
      int leakLevel,
      boolean modeChanges,
      boolean showProgress) {

    super(id, name, required, visible, owner, proximityMetric);
    this.region = region;
    this.material = material;
    this.leakLevel = leakLevel;
    this.modeChanges = modeChanges;
    this.showProgress = showProgress;
  }

  public Region getRegion() {
    return this.region;
  }

  public MaterialData getMaterial() {
    return this.material;
  }

  public int getLeakLevel() {
    return this.leakLevel;
  }

  public boolean hasModeChanges() {
    return this.modeChanges;
  }

  public boolean getShowProgress() {
    return this.showProgress;
  }
}
