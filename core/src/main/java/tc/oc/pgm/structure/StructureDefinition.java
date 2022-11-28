package tc.oc.pgm.structure;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.regions.Bounds;

@FeatureInfo(name = "structure")
public class StructureDefinition extends SelfIdentifyingFeatureDefinition {

  private final Region region;
  private final boolean includeAir;
  private final boolean clearSource;

  // Lazy init due to yet unresolved xml references
  private Vector origin;
  private Bounds bounds;

  public StructureDefinition(
      String id, @Nullable Vector origin, Region region, boolean includeAir, boolean clearSource) {
    super(id);
    this.origin = origin;
    this.region = assertNotNull(region);
    this.includeAir = includeAir;
    this.clearSource = clearSource;
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

  public Vector getOrigin() {
    if (origin == null) {
      this.origin = getBounds().getMin();
    }
    return this.origin;
  }

  public Bounds getBounds() {
    if (bounds == null) {
      this.bounds = region.getBounds();
    }
    return bounds;
  }
}
