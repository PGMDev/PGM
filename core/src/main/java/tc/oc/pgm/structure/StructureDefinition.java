package tc.oc.pgm.structure;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "structure")
public class StructureDefinition extends SelfIdentifyingFeatureDefinition {

  private final Region region;
  private Vector origin; // not final because of possible xml references
  private final boolean includeAir;
  private final boolean clearSource;

  public StructureDefinition(
      String id, @Nullable Vector origin, Region region, boolean includeAir, boolean clearSource) {
    super(id);
    this.origin = origin;
    this.region = checkNotNull(region);
    this.includeAir = includeAir;
    this.clearSource = clearSource;
  }

  public Vector getOrigin() {
    if (origin == null) {
      this.origin = region.getBounds().getMin();
    }
    return this.origin;
  }

  public Region getRegion() {
    return region;
  }

  public boolean includeAir() {
    return includeAir;
  }

  public boolean clearSource() {
    return clearSource;
  }
}
