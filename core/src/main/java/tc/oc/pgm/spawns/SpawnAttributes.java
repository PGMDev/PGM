package tc.oc.pgm.spawns;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointProviderAttributes;

public class SpawnAttributes {
  public final Filter filter;
  public final PointProviderAttributes providerAttributes;
  public final Kit kit;
  public final boolean sequential;
  public final boolean spread;
  public final boolean spreadTeammates;
  public final boolean exclusive;
  public final boolean persistent;

  public SpawnAttributes(
      Filter filter,
      PointProviderAttributes providerAttributes,
      Kit kit,
      boolean sequential,
      boolean spread,
      boolean spreadTeammates,
      boolean exclusive,
      boolean persistent) {
    this.filter = filter;
    this.providerAttributes = providerAttributes;
    this.kit = kit;
    this.sequential = sequential;
    this.spread = spread;
    this.spreadTeammates = spreadTeammates;
    this.exclusive = exclusive;
    this.persistent = persistent;
  }

  public SpawnAttributes() {
    this(
        StaticFilter.ABSTAIN,
        new PointProviderAttributes(),
        null,
        false,
        false,
        false,
        false,
        false);
  }
}
