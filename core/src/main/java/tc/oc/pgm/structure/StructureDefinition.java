package tc.oc.pgm.structure;

import static tc.oc.pgm.util.Assert.assertNotNull;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.regions.Bounds;

@FeatureInfo(name = "structure")
public class StructureDefinition extends SelfIdentifyingFeatureDefinition {

  @Getter
  private final Region region;

  @Accessors(fluent = true)
  @Getter
  private final boolean includeAir;

  @Accessors(fluent = true)
  @Getter
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

  public Vector getOrigin() {
    if (origin == null) {
      this.origin = getBounds().getMin();
    }
    return this.origin.clone();
  }

  public Bounds getBounds() {
    if (bounds == null) {
      this.bounds = region.getBounds();
    }
    return bounds;
  }

  public Structure getStructure(Match match) {
    return match.getFeatureContext().get(this.getId(), Structure.class);
  }
}
