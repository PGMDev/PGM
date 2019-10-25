package tc.oc.pgm.core;

import javax.annotation.Nullable;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.features.FeatureInfo;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "core")
public class CoreFactory extends ProximityGoalDefinition {
  protected final Region region;
  protected final MaterialData material;
  protected final int leakLevel;
  protected final boolean modeChanges;

  public CoreFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      TeamFactory owner,
      ProximityMetric proximityMetric,
      Region region,
      MaterialData material,
      int leakLevel,
      boolean modeChanges) {

    super(id, name, required, visible, owner, proximityMetric);
    this.region = region;
    this.material = material;
    this.leakLevel = leakLevel;
    this.modeChanges = modeChanges;
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
}
