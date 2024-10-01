package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableSet;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;

@FeatureInfo(name = "destroyable")
public class DestroyableFactory extends ProximityGoalDefinition {
  public static enum SparksType {
    SPARKS_ALL("true"),
    SPARKS_NONE("false"),
    SPARKS_NEAR("near");

    private final String name;

    private SparksType(String name) {
      this.name = name;
    }

    public static SparksType fromString(String name, Element el) throws InvalidXMLException {
      for (SparksType i : SparksType.values()) {
        if (i.name.equalsIgnoreCase(name)) {
          return i;
        }
      }
      throw new InvalidXMLException("Unknown Destroyable sparks type: " + name, el);
    }
  }

  protected final Region region;
  protected final MaterialMatcher materials;
  protected final double destructionRequired;
  protected final ImmutableSet<Mode> modeList;
  protected final boolean showProgress;
  protected final SparksType sparks;
  protected final boolean repairable;

  public DestroyableFactory(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      TeamFactory owner,
      ProximityMetric proximityMetric,
      Region region,
      MaterialMatcher materials,
      double destructionRequired,
      @Nullable ImmutableSet<Mode> modeList,
      boolean showProgress,
      String sparks,
      boolean repairable,
      Element el)
      throws InvalidXMLException {
    super(id, name, required, showOptions, owner, proximityMetric);
    this.region = region;
    this.materials = materials;
    this.destructionRequired = destructionRequired;
    this.modeList = modeList;
    this.showProgress = showProgress;
    this.sparks = SparksType.fromString(sparks, el);
    this.repairable = repairable;
  }

  public Region getRegion() {
    return this.region;
  }

  public MaterialMatcher getMaterials() {
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

  public boolean isSparksActive() {
    return this.sparks != SparksType.SPARKS_NONE;
  }

  public boolean isSparksAll() {
    return this.sparks != SparksType.SPARKS_NEAR;
  }

  public boolean isRepairable() {
    return this.repairable;
  }
}
