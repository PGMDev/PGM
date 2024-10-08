package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.Aliased;
import tc.oc.pgm.util.material.MaterialMatcher;

@FeatureInfo(name = "destroyable")
public class DestroyableFactory extends ProximityGoalDefinition {
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
      SparksType sparks,
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
    return this.sparks != SparksType.NONE;
  }

  public boolean isSparksAll() {
    return this.sparks == SparksType.ALL;
  }

  public boolean isRepairable() {
    return this.repairable;
  }

  public enum SparksType implements Aliased {
    NONE("false", "no", "off"),
    NEAR("near"),
    ALL("true", "yes", "on");

    private final String[] names;

    SparksType(String... names) {
      this.names = names;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
      return Iterators.forArray(names);
    }
  }
}
