package tc.oc.pgm.regions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RFAContext {
  private final Multimap<RFAScope, RegionFilterApplication> rfas;
  private final List<RegionFilterApplication> byPriority;

  public RFAContext(Iterable<RegionFilterApplication> byPriority) {
    this.byPriority = ImmutableList.copyOf(byPriority);
    ImmutableListMultimap.Builder<RFAScope, RegionFilterApplication> rfaBuilder =
        ImmutableListMultimap.builder();
    for (RegionFilterApplication rfa : this.byPriority) {
      rfaBuilder.put(rfa.scope, rfa);
    }
    this.rfas = rfaBuilder.build();
  }

  /** Return all RFAs in the given scope, in priority order */
  public Iterable<RegionFilterApplication> get(RFAScope scope) {
    return this.rfas.get(scope);
  }

  /** Return all RFAs in priority order */
  public Iterable<RegionFilterApplication> getAll() {
    return this.byPriority;
  }

  public static class Builder extends RFAContext {
    private final List<RegionFilterApplication> byPriority = new ArrayList<>();

    public Builder() {
      super(Collections.emptyList());
    }

    /** Append the given RFA, giving it the lowest priority */
    public void add(RegionFilterApplication rfa) {
      this.byPriority.add(rfa);
    }

    /** Prepend the given RFA, giving it the highest priority */
    public void prepend(RegionFilterApplication rfa) {
      rfa.useRegionPriority = true; // Allows region priority to work on older maps
      this.byPriority.add(0, rfa);
    }

    @Override
    public Iterable<RegionFilterApplication> get(RFAScope scope) {
      throw new UnsupportedOperationException("Cannot call get without building first!");
    }

    @Override
    public Iterable<RegionFilterApplication> getAll() {
      throw new UnsupportedOperationException("Cannot call getAll without building first!");
    }

    public RFAContext build() {
      return new RFAContext(byPriority);
    }
  }
}
