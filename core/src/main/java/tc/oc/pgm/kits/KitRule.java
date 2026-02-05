package tc.oc.pgm.kits;

import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;

public record KitRule(Action action, Kit kit, Filter filter) implements FeatureDefinition {

  enum Action {
    GIVE,
    TAKE,
    LEND
  }

  @Deprecated
  public Action getAction() {
    return action();
  }

  @Deprecated
  public Kit getKit() {
    return kit();
  }

  @Deprecated
  public Filter getFilter() {
    return filter();
  }
}
