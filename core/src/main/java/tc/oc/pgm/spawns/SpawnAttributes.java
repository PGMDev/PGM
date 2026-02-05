package tc.oc.pgm.spawns;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointProviderAttributes;

public record SpawnAttributes(
    Filter filter,
    PointProviderAttributes providerAttributes,
    Kit kit,
    boolean sequential,
    boolean spread,
    boolean spreadTeammates,
    boolean exclusive,
    boolean persistent) {

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
